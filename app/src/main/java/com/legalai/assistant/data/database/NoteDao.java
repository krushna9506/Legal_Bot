package com.legalai.assistant.data.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.legalai.assistant.data.model.NoteItem;

import java.util.List;

@Dao
public interface NoteDao {
    @Query("SELECT * FROM notes ORDER BY updatedAt DESC")
    List<NoteItem> getAllNotes();

    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    NoteItem getNoteById(int id);

    @Query("SELECT * FROM notes WHERE title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%' ORDER BY updatedAt DESC")
    List<NoteItem> searchNotes(String query);

    @Query("SELECT COUNT(*) FROM notes")
    int getNotesCount();

    @Insert
    long insert(NoteItem note);

    @Update
    void update(NoteItem note);

    @Delete
    void delete(NoteItem note);
}
