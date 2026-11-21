package com.legalai.assistant.data.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.legalai.assistant.data.model.TaskItem;

import java.util.List;

@Dao
public interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY isCompleted ASC, id DESC")
    List<TaskItem> getAllTasks();

    @Query("SELECT * FROM tasks WHERE isCompleted = 0 ORDER BY id DESC")
    List<TaskItem> getPendingTasks();

    @Query("SELECT * FROM tasks WHERE isCompleted = 1 ORDER BY id DESC")
    List<TaskItem> getCompletedTasks();

    @Query("SELECT COUNT(*) FROM tasks WHERE isCompleted = 0")
    int getPendingCount();

    @Query("SELECT COUNT(*) FROM tasks WHERE isCompleted = 1")
    int getCompletedCount();

    @Insert
    long insert(TaskItem task);

    @Update
    void update(TaskItem task);

    @Delete
    void delete(TaskItem task);
}
