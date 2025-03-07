/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.bcaleaguetableassigner;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.IntStream;
import java.util.stream.Collectors;
import javax.swing.table.DefaultTableModel;

/**
 *
 * @author c_dra
 */
public class TableAssigner {

    private final Random rng;
    private final Schedule schedule;
    private final int numberOfMatches;
    private final int tableDistributionLimit;

    private List<LeagueTeam> teams;
    private Map<String, String> byeTeams;
    private DefaultTableModel tableAssignments;

    public TableAssigner(Schedule schedule) {
        rng = new Random(System.currentTimeMillis());
        this.schedule = schedule;
        numberOfMatches = schedule.getAllScheduledRecords()
            .stream()
            .max(Comparator.comparingInt(ScheduleRecord::getLeagueMatchesCount)).get().getLeagueMatchesCount();
        tableDistributionLimit = BigDecimal.valueOf(
                schedule.getNumberOfWeeksInSchedule() / (double) numberOfMatches).setScale(0, RoundingMode.UP)
            .intValue();
        
        initializeLeagueTeams();
        initializeTableAssignments();
        assignTablesV1();
    }
    
    public DefaultTableModel getTableAssignments() {
        return tableAssignments;
    }

    private void initializeLeagueTeams() {
        teams = new ArrayList<>();
        for (int i = 1; i <= numberOfMatches * 2; i++) {
            teams.add(new LeagueTeam(i, numberOfMatches));
        }
        
        byeTeams = schedule.getByeTeams();
    }

    private void initializeTableAssignments() {
        int tableNumber = 1;
        tableAssignments = new DefaultTableModel();
        tableAssignments.addColumn("Week");
        for (int i = 0; i < numberOfMatches; i++) {
            tableAssignments.addColumn(String.format("%s", tableNumber++));
        }
        
        for (int i = 1; i <= schedule.getNumberOfWeeksInSchedule(); i++) {
            tableAssignments.addRow(new String[numberOfMatches]);
        }
    }

    private void assignTablesV1() {
        schedule.getAllScheduledRecords().stream().forEach(sr ->
        {
            tableAssignments.setValueAt(String.format("%"+String.valueOf(schedule.getNumberOfWeeksInSchedule()).length() + "s", sr.getLeagueWeek()), 
                    sr.getLeagueWeek() - 1, 0);
            
            List<LeagueMatch> matches = sr.getAllLeagueMatches();
            List<LeagueMatch> actualMatches = matches.stream().filter(lm -> !byeTeams.containsKey(String.valueOf(lm.getAwayTeam())) &&
                    !byeTeams.containsKey(String.valueOf(lm.getHomeTeam()))).toList();
            List<LeagueMatch> byeMatches = matches.stream().filter(lm -> byeTeams.containsKey(String.valueOf(lm.getAwayTeam())) ||
                    byeTeams.containsKey(String.valueOf(lm.getHomeTeam()))).toList();
            
            List<Integer> availableTables = IntStream.rangeClosed(1, actualMatches.size())
                    .boxed()
                    .collect(Collectors.toList());
                        
            // Assign actual matches first
            for (int i = 0; i < actualMatches.size(); i++) {
                LeagueMatch lm = actualMatches.get(i);
                LeagueTeam awayTeam = teams.stream().filter(t -> t.getId() == lm.getAwayTeam()).findFirst().get();
                LeagueTeam homeTeam = teams.stream().filter(t -> t.getId() == lm.getHomeTeam()).findFirst().get();
                
                Map<Integer, Integer> weights = new HashMap<>();
                for (int table : availableTables) {
                    weights.put(table, awayTeam.getTableWeight(table) + homeTeam.getTableWeight(table));
                }

                weights = weights.entrySet().stream()
                        .filter(w -> w.getValue() <= tableDistributionLimit)
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
                
                if (weights.isEmpty()) {
                    for (int table : availableTables) {
                        weights.put(table, awayTeam.getTableWeight(table) + homeTeam.getTableWeight(table));
                    }
                }
                
                int lowestWeight = Collections.min(weights.values());
                List<Integer> lowestTables = weights.entrySet().stream()
                        .filter(x -> x.getValue() == lowestWeight)
                        .map(Map.Entry::getKey)
                        .collect(Collectors.toList());
                int tableIndex = rng.nextInt(lowestTables.size());
                int chosenTable = lowestTables.get(tableIndex);

                tableAssignments.setValueAt(lm.getLeagueMatch(), sr.getLeagueWeek() - 1, chosenTable);
                awayTeam.incrementTableWeight(chosenTable);
                homeTeam.incrementTableWeight(chosenTable);
                availableTables.remove(availableTables.indexOf(chosenTable));
            }
            
            // Assign BYE matches in the right most available cells of the current week
            for (int i = 0; i < byeMatches.size(); i++) {
                for (int j = 0; j < tableAssignments.getColumnCount(); j++) {
                    if (tableAssignments.getValueAt(sr.getLeagueWeek() - 1, j) == null) {
                        tableAssignments.setValueAt(byeMatches.get(i).getLeagueMatch(), sr.getLeagueWeek() - 1, j);
                        break;
                    }
                }
            }
        });
    }
}
