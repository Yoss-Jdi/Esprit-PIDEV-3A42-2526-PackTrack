package com.gestioncolis.services;
import java.sql.SQLException;
import java.util.List;

public interface ICrudCL<T> {
    void ajouter(T t)    throws Exception;
    void modifier(T t)   throws Exception;
    void supprimer(int id) throws Exception;
    List<T> getAll()     throws SQLException;
}