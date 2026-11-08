package com.iiss.sensores.grupo_2.controlador.engine.service;

import com.iiss.sensores.grupo_2.controlador.engine.persistence.TemperatureReadingRepository;
import com.iiss.sensores.grupo_2.controlador.engine.utils.CustomClock;
import com.iiss.sensores.grupo_2.dto.TemperatureMessage;
import com.iiss.sensores.grupo_2.modelos.input.DataSite;
import com.iiss.sensores.grupo_2.modelos.input.Room;
import com.iiss.sensores.grupo_2.enums.SwitchState;
import com.iiss.sensores.grupo_2.controlador.engine.client.SwitchClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Keeps a complete in-memory copy of the site state (with temperatures and switch status) */
@Service
public class SiteStateService {
  private static final Logger logger = LoggerFactory.getLogger(SiteStateService.class);
  private DataSite dataSite;
  private final SwitchClient switchClient;
  private final TemperatureReadingRepository tempReadings;

  /**
   * IDs of the rooms whose runtime state (latest temperature and switch status) has not been loaded
   * yet. Only these rooms are queried against the DB and the switches API.
   */
  private final Set<String> pendingHydration = new LinkedHashSet<>();

  /** Minutes without readings after which a thermostat is considered silent (stale) */
  @Value("${controlador.lectura-max-antiguedad-min:15}")
  private long lecturaMaxAntiguedadMin = 15;

  /** Arrival time of the latest reading of each thermostat, by thermostat ID (see {@link #now()}) */
  private final Map<String, Instant> lastReadingAt = new HashMap<>();

  /** Control clock, set by the engine while it runs; null while stopped */
  private volatile CustomClock clock;

  /** Rooms removed from the config (or whose switch changed) while their switch was ON */
  private final List<Room> orphanRooms = new ArrayList<>();

  /** An orphan whose switch can't be turned OFF within this time is given up (one final error) */
  private static final Duration ORPHAN_MAX_AGE = Duration.ofHours(1);

  private final Map<String, Instant> orphanSince = new HashMap<>();

  public SiteStateService(SwitchClient switchClient, TemperatureReadingRepository tempReadings) {
    this.switchClient = switchClient;
    this.tempReadings = tempReadings;
  }

  /** The silence of thermostats is measured with the control clock (null: the system clock) */
  public void setClock(CustomClock clock) {
    this.clock = clock;
  }

  private Instant now() {
    CustomClock current = clock;
    return current == null ? Instant.now() : current.getTime();
  }

  public synchronized void loadSiteState(DataSite newSiteState) {
    this.loadSiteState(newSiteState, false);
  }

  /**
   * Initializes or replaces the site config state.
   *
   * <p>Rooms already known with the same hardware (switch and thermostat) keep their runtime state
   * (temperature, switch status and {@link Room#lastSwitchChange()}), taking only the new config.
   * New rooms, or rooms whose hardware changed, are hydrated lazily on the next {@link
   * #getSiteState} with the latest temperature reading from DB and switch status from its API.
   *
   * @param newSiteState the complete new config for the site
   * @param fullReplace if true, replaces the entire site state, discarding any previous runtime
   *     state; if false, only updates the config, keeping runtime state for rooms with the same
   *     hardware
   */
  public synchronized void loadSiteState(DataSite newSiteState, boolean fullReplace) {
    if (newSiteState == null
        || newSiteState.site() == null
        || newSiteState.habitaciones() == null) {
      throw new IllegalArgumentException("The Site state cannot be empty nor null");
    }
    // A new config (e.g. a fixed switch URL) deserves an immediate retry, not waiting for a backoff
    switchClient.resetBackoff();
    if (fullReplace) {
      trackOrphanRooms(newSiteState.habitaciones());
      lastReadingAt.clear();
      this.dataSite = new DataSite(newSiteState.site(), List.copyOf(newSiteState.habitaciones()));
      pendingHydration.clear();
      newSiteState.habitaciones().forEach(room -> pendingHydration.add(room.id()));
      return;
    }

    Map<String, Room> currentRooms = new HashMap<>();
    if (dataSite != null) {
      dataSite.habitaciones().forEach(room -> currentRooms.put(room.id(), room));
    }

    List<Room> mergedRooms = new ArrayList<>();
    Set<String> newPending = new LinkedHashSet<>();
    for (Room newRoom : newSiteState.habitaciones()) {
      Room currentRoom = currentRooms.get(newRoom.id());
      if (currentRoom != null && sameHardware(currentRoom, newRoom)) {
        mergedRooms.add(withRuntimeStateOf(newRoom, currentRoom));
        if (pendingHydration.contains(newRoom.id())) {
          newPending.add(newRoom.id());
        }
      } else {
        mergedRooms.add(newRoom);
        newPending.add(newRoom.id());
      }
    }

    trackOrphanRooms(mergedRooms);
    this.dataSite = new DataSite(newSiteState.site(), List.copyOf(mergedRooms));
    pendingHydration.clear();
    pendingHydration.addAll(newPending);
  }

  private static String switchKey(Room room) {
    return room.idSwitch() + "|" + room.urlSwitch();
  }

  /**
   * Remembers the rooms of the current state whose switch is ON and no longer belongs to any room
   * of {@code newRooms}, so the engine can turn it OFF instead of leaving it heating unattended.
   */
  private void trackOrphanRooms(List<Room> newRooms) {
    if (dataSite == null) {
      return;
    }
    Set<String> keptSwitches = new HashSet<>();
    newRooms.forEach(room -> keptSwitches.add(switchKey(room)));
    orphanRooms.removeIf(orphan -> keptSwitches.contains(switchKey(orphan)));
    for (Room old : dataSite.habitaciones()) {
      boolean alreadyTracked =
          orphanRooms.stream().anyMatch(orphan -> switchKey(orphan).equals(switchKey(old)));
      if (old.currentSwitchState() == SwitchState.ENCENDIDO
          && !keptSwitches.contains(switchKey(old))
          && !alreadyTracked) {
        orphanRooms.add(old);
        orphanSince.put(switchKey(old), Instant.now());
      }
    }
    orphanSince.keySet().removeIf(key -> orphanRooms.stream().noneMatch(o -> switchKey(o).equals(key)));
  }

  /** Rooms removed from the config whose switch was ON and still has to be turned OFF */
  public synchronized List<Room> getOrphanRooms() {
    Instant limit = Instant.now().minus(ORPHAN_MAX_AGE);
    orphanRooms.removeIf(
        orphan -> {
          Instant since = orphanSince.get(switchKey(orphan));
          if (since != null && since.isBefore(limit)) {
            logger.error(
                "No se pudo apagar el switch {} de una habitación eliminada; se deja de reintentar",
                orphan.idSwitch());
            return true;
          }
          return false;
        });
    return List.copyOf(orphanRooms);
  }

  public synchronized void removeOrphanRoom(Room room) {
    orphanRooms.removeIf(orphan -> switchKey(orphan).equals(switchKey(room)));
    orphanSince.remove(switchKey(room));
  }

  /**
   * Rooms with their switch ON whose thermostat is silent: no reading at all or none during the
   * last {@code controlador.lectura-max-antiguedad-min} minutes. They can't be controlled safely.
   */
  public synchronized List<Room> getStaleOnRooms() {
    if (dataSite == null) {
      return List.of();
    }
    return dataSite.habitaciones().stream()
        .filter(this::isHydrated)
        .filter(room -> room.currentSwitchState() == SwitchState.ENCENDIDO)
        .filter(this::isStale)
        .toList();
  }

  private boolean isStale(Room room) {
    Instant last = lastReadingAt.get(room.idThermostat());
    if (last == null) {
      return room.currentTemperatureC() == null;
    }
    Duration maxAge = Duration.ofMinutes(lecturaMaxAntiguedadMin);
    return Duration.between(last, now()).compareTo(maxAge) > 0;
  }

  private static boolean sameHardware(Room a, Room b) {
    return Objects.equals(a.idSwitch(), b.idSwitch())
        && Objects.equals(a.urlSwitch(), b.urlSwitch())
        && Objects.equals(a.idThermostat(), b.idThermostat());
  }

  /** Config from {@code config}, runtime state from {@code runtime} */
  private static Room withRuntimeStateOf(Room config, Room runtime) {
    return new Room(
        config.id(),
        config.name(),
        config.expectedTemperature(),
        config.powerKW(),
        config.idThermostat(),
        config.topicThermostat(),
        config.idSwitch(),
        config.urlSwitch(),
        runtime.currentTemperatureC(),
        runtime.currentSwitchState(),
        runtime.lastSwitchChange());
  }

  private void checkInitializedSiteState() {
    if (dataSite == null) {
      throw new IllegalStateException("The Site state is not yet been initialized");
    }
  }

  private void replaceSiteRooms(List<Room> rooms) {
    dataSite = new DataSite(dataSite.site(), List.copyOf(rooms));
  }

  /**
   * Gets the entire site config state.
   *
   * @return The complete config for the site, with latest temperature readings and switches
   *     statuses
   */
  public synchronized DataSite getSiteState() {
    return getSiteState(null);
  }

  /**
   * Gets the entire site config state, keeping up to date the moment each room's current situation
   * started (see {@link Room#lastSwitchChange()}) so the Core can prevent starvation:
   *
   * <ul>
   *   <li>rooms whose start is unknown are stamped with {@code now}
   *   <li>OFF rooms at or over their expected temperature are not waiting, so their clock restarts
   * </ul>
   *
   * @param now current (simulated) time, or null to skip stamping
   */
  public synchronized DataSite getSiteState(ZonedDateTime now) {
    checkInitializedSiteState();
    if (!pendingHydration.isEmpty()) {
      hydratePendingRooms();
    }

    if (now != null) {
      replaceSiteRooms(
          dataSite.habitaciones().stream()
              .map(room -> isHydrated(room) ? stampSituationStart(room, now) : room)
              .toList());
    }

    // Skip rooms not hydrated yet or with no temperature reading
    return new DataSite(
        dataSite.site(),
        dataSite.habitaciones().stream()
            .filter(this::isHydrated)
            .filter(room -> room.currentTemperatureC() != null)
            .filter(room -> !isStale(room))
            .toList());
  }

  private boolean isHydrated(Room room) {
    return !pendingHydration.contains(room.id());
  }

  /**
   * Loads latest temperature reading and switch status only for the rooms pending hydration. A room
   * whose switch cannot be queried stays pending and is retried on the next call.
   */
  private void hydratePendingRooms() {
    List<Room> pendingRooms = dataSite.habitaciones().stream().filter(r -> !isHydrated(r)).toList();
    List<String> idThermostats = pendingRooms.stream().map(Room::idThermostat).toList();
    Map<String, Double> latestTemps = new HashMap<>();
    Instant oldestValid = Instant.now().minus(Duration.ofMinutes(lecturaMaxAntiguedadMin));
    for (TemperatureMessage reading : tempReadings.getLastTemperatureMeasurements(idThermostats)) {
      // Stale readings from the DB are ignored: the room has no temperature until a new one
      if (reading.ts() != null && reading.ts().isBefore(oldestValid)) {
        continue;
      }
      if (latestTemps.putIfAbsent(reading.idThermostat(), reading.tC()) == null) {
        lastReadingAt.putIfAbsent(
            reading.idThermostat(), reading.ts() != null ? reading.ts() : Instant.now());
      }
    }

    Map<String, Room> hydrated = new HashMap<>();
    for (Room room : pendingRooms) {
      try {
        SwitchState switchState = queryStateOrTurnOff(room);
        hydrated.put(
            room.id(),
            new Room(
                room.id(),
                room.name(),
                room.expectedTemperature(),
                room.powerKW(),
                room.idThermostat(),
                room.topicThermostat(),
                room.idSwitch(),
                room.urlSwitch(),
                knownTemperatureOr(room, latestTemps.get(room.idThermostat())),
                switchState,
                room.lastSwitchChange()));
      } catch (Exception e) {
        if (e instanceof SwitchClient.SwitchInBackoffException) {
          logger.debug("Switch {} (room {}) is in backoff, will retry later", room.idSwitch(), room.id());
        } else {
          logger.warn(
              "Could not get state of switch {} (room {}), will retry on next cycle",
              room.idSwitch(),
              room.id());
          logger.debug("Error: ", e);
        }
      }
    }

    replaceSiteRooms(
        dataSite.habitaciones().stream().map(r -> hydrated.getOrDefault(r.id(), r)).toList());
    pendingHydration.removeAll(hydrated.keySet());
  }

  /**
   * Queries the switch state. If it can't be known (query error, response without state, etc.) the
   * switch is sent an OFF command just in case, and when that succeeds it is known to be OFF.
   */
  private SwitchState queryStateOrTurnOff(Room room) throws Exception {
    try {
      return switchClient.consultarEstado(room).estado();
    } catch (Exception queryError) {
      logger.warn(
          "Could not get state of switch {} (room {}), sending OFF just in case",
          room.idSwitch(),
          room.id());
      logger.debug("Error: ", queryError);
      switchClient.apagar(room);
      return SwitchState.APAGADO;
    }
  }

  /** All rooms with their last known state, including those not hydrated or without readings */
  public synchronized List<Room> getAllRooms() {
    checkInitializedSiteState();
    return dataSite.habitaciones();
  }

  /** A reading already received via MQTT while pending is newer than (or equal to) the DB one */
  private static Double knownTemperatureOr(Room room, Double fallback) {
    Double known = room.currentTemperatureC();
    return known != null && Double.isFinite(known) ? known : fallback;
  }

  private static Room stampSituationStart(Room room, ZonedDateTime now) {
    boolean satisfiedAndOff =
        room.currentSwitchState() == SwitchState.APAGADO
            && room.currentTemperatureC() != null
            && room.currentTemperatureC() >= room.expectedTemperature();
    if (room.lastSwitchChange() == null || satisfiedAndOff) {
      return room.withLastSwitchChange(now);
    }
    return room;
  }

  /** Updates the temperature received from a thermostat */
  public synchronized DataSite updateRoomTemperature(String idThermostat, double temperature) {
    checkInitializedSiteState();
    List<Room> habitaciones = new ArrayList<>(dataSite.habitaciones());
    boolean roomFound = false;

    if (idThermostat == null || idThermostat.isBlank() || !Double.isFinite(temperature)) {
      throw new IllegalArgumentException("Invalid thermostat ID or temperature");
    }

    for (int i = 0; i < habitaciones.size(); i++) {
      Room h = habitaciones.get(i);
      if (h.idThermostat().equals(idThermostat)) {
        habitaciones.set(
            i,
            new Room(
                h.id(),
                h.name(),
                h.expectedTemperature(),
                h.powerKW(),
                h.idThermostat(),
                h.topicThermostat(),
                h.idSwitch(),
                h.urlSwitch(),
                temperature,
                h.currentSwitchState(),
                h.lastSwitchChange()));
        roomFound = true;
      }
    }

    if (!roomFound) {
      throw new IllegalArgumentException("No Room associated with the thermostat: " + idThermostat);
    }
    lastReadingAt.put(idThermostat, now());
    replaceSiteRooms(habitaciones);
    return dataSite;
  }

  /** Updates known state of a switch, keeping its situation start unless the state changed */
  public synchronized DataSite updateSwitchState(String idSwitch, SwitchState newSwitchState) {
    return updateSwitchState(idSwitch, newSwitchState, null);
  }

  /**
   * Updates known state of a switch.
   *
   * @param changedAt (simulated) moment of the change; when the state actually changes it becomes
   *     the room's {@code lastSwitchChange} (null leaves it unknown)
   */
  public synchronized DataSite updateSwitchState(
      String idSwitch, SwitchState newSwitchState, ZonedDateTime changedAt) {
    checkInitializedSiteState();
    List<Room> rooms = new ArrayList<>(dataSite.habitaciones());
    boolean roomFound = false;

    if (idSwitch == null || idSwitch.isBlank() || newSwitchState == null) {
      throw new IllegalArgumentException("Invalid switch ID or new switch state");
    }

    for (int i = 0; i < rooms.size(); i++) {
      Room h = rooms.get(i);
      if (h.idSwitch().equals(idSwitch)) {
        rooms.set(
            i,
            new Room(
                h.id(),
                h.name(),
                h.expectedTemperature(),
                h.powerKW(),
                h.idThermostat(),
                h.topicThermostat(),
                h.idSwitch(),
                h.urlSwitch(),
                h.currentTemperatureC(),
                newSwitchState,
                h.currentSwitchState() == newSwitchState ? h.lastSwitchChange() : changedAt));
        roomFound = true;
        break;
      }
    }

    if (!roomFound) {
      throw new IllegalArgumentException("No Room associated with the switch:  " + idSwitch);
    }
    replaceSiteRooms(rooms);
    return dataSite;
  }
}
