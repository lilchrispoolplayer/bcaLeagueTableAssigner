/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.bca9balltableassigner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author c_dra
 */
public class Schedule {

    private List<String> schedule;
    private List<ScheduleRecord> scheduleRecords;

    public Schedule(String scheduleFile) {
        loadSchedule(scheduleFile);
    }

    private void loadSchedule(String scheduleFile) {
        try {
            // File must be in CSV format
            schedule = Files.readAllLines(Paths.get(scheduleFile));
        } catch (IOException ioe) {

        }
    }
    
    public boolean validateSchedule() {
        boolean valid = true;

        String[] recordPieces = schedule.get(0).split(",");
        for (String record : schedule) {
            // Check the record contains commas
            if (!record.contains(",")) {
                valid = false;
                break;
            }

            // Check the record follows the format
            if (record.matches("^\\d+(,\\d+ @ \\d+)+$")) {
                valid = false;
                break;
            }
            
            // Check that records are the same length
            if (record.split(",").length != recordPieces.length) {
                valid = false;
                break;
            }
        }

        return valid;
    }

    public void processSchedule() {
        scheduleRecords = new ArrayList<>();
        schedule.stream().forEach(record -> scheduleRecords.add(new ScheduleRecord(record, schedule.size())));
    }

    public ScheduleRecord getScheduledRecord(int index) {
        if (index < 0 || scheduleRecords.size() - 1 < index) {
            return null;
        }

        return scheduleRecords.get(index);
    }

    public List<ScheduleRecord> getAllScheduledRecords() {
        return scheduleRecords;
    }

    public int getNumberOfWeeksInSchedule() {
        return scheduleRecords.size();
    }
}
