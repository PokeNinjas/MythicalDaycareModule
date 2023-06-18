package com.mythicalnetwork.mythicaldaycare.database;

import com.mythicalnetwork.mythicaldaycare.MythicalDaycare;
import com.mythicalnetwork.mythicaldaycare.daycare.Egg;
import com.mythicalnetwork.mythicaldaycare.daycare.PastureInstance;
import com.pokeninjas.kingdoms.fabric.dto.database.impl.User;
import com.pokeninjas.kingdoms.fabric.exception.UserOfflineException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;


public class DaycareUser {
    public String pasture = null;
    public List<String> eggs = new ArrayList<>();

    public PastureInstance getPasture() {
        return MythicalDaycare.INSTANCE.getGSON().fromJson(pasture, PastureInstance.class);
    }

    public List<Egg> getEggs() {
        return eggs.stream().map(it -> MythicalDaycare.INSTANCE.getGSON().fromJson(it, Egg.class)).collect(Collectors.toList());
    }

    public void setPastureData(UUID uuid, PastureInstance data) {
        pasture = MythicalDaycare.INSTANCE.getGSON().toJson(data);
        save(uuid);
    }

    public void setEggData(UUID uuid, List<Egg> data) {
        eggs = data.stream().map(it -> MythicalDaycare.INSTANCE.getGSON().toJson(it)).collect(Collectors.toList());
        save(uuid);
    }

    public void save(UUID uuid) {
        try {
            User.get(uuid).setData("mythical_daycare_user", this);
        } catch (UserOfflineException e) {
            MythicalDaycare.INSTANCE.getLOGGER().error("Error while attempting to save Daycare data for user: " + uuid);
            e.printStackTrace();
        }
    }

    @Override
    public String toString() {
        return "BreedingUser{" +
                ", pasture='" + pasture + '\'' +
                ", eggs='" + eggs + '\'' +
                '}';
    }
}
