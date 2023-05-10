package com.mythicalnetwork.mythicaldaycare.database;

import com.mythicalnetwork.mythicaldaycare.MythicalDaycare;
import com.mythicalnetwork.mythicaldaycare.daycare.Egg;
import com.mythicalnetwork.mythicaldaycare.daycare.PastureInstance;
import dev.lightdream.databasemanager.DatabaseMain;
import dev.lightdream.databasemanager.dto.DatabaseEntry;
import jakarta.persistence.*;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Entity
@Table(name = "users",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"uuid"})
        }
)
public class DaycareUser extends DatabaseEntry {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, unique = true, length = 11)
    public int id;

    @Column(name = "uuid", unique = true)
    public UUID uuid = null;

    @Column(name = "name")
    public String name = null;

    @Lob
    @Column(name = "pasture", columnDefinition = "BLOB")
    public String pasture = null;

    @Lob
    @Column(name = "eggs", columnDefinition = "BLOB")
    public List<String> eggs = new ArrayList<>();

    public DaycareUser(UUID uuid, String name) {
        super(MythicalDaycare.INSTANCE);
        this.uuid = uuid;
        this.name = name;
    }

    public DaycareUser() {
        super(MythicalDaycare.INSTANCE);
    }

    public DaycareUser(ServerPlayer player) {
        this(player.getUUID(), player.getName().getString());
    }

    public static DaycareUser getUser(ServerPlayer player) {
        return MythicalDaycare.INSTANCE.getDatabaseManager(true).getUser(player);
    }

    public UUID getUUID() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public PastureInstance getPasture() {
        return DatabaseManager.Companion.getGSON().fromJson(pasture, PastureInstance.class);
    }

    public List<Egg> getEggs() {
        return eggs.stream().map(it -> DatabaseManager.Companion.getGSON().fromJson(it, Egg.class)).collect(Collectors.toList());
    }

    public void setUUID(UUID uuid) {
        this.uuid = uuid;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPastureData(PastureInstance data) {
        pasture = DatabaseManager.Companion.getGSON().toJson(data);
        save();
    }

    public void setEggData(List<Egg> data) {
        eggs = data.stream().map(it -> DatabaseManager.Companion.getGSON().toJson(it)).collect(Collectors.toList());
        save();
    }

    @Override
    public int getID() {
        return id;
    }

    @Override
    public String toString() {
        return "BreedingUser{" +
                "id=" + id +
                ", uuid=" + uuid +
                ", name='" + name + '\'' +
                ", pasture='" + pasture + '\'' +
                ", eggs='" + eggs + '\'' +
                '}';
    }
}
