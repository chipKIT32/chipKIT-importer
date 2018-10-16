package com.microchip.mplab.nbide.embedded.arduino.importer;

import java.util.Objects;

public final class BoardId {
    
    private final String board;
    private final String cpu;

    public BoardId(String board) {
        this(board, "");
    }
    
    public BoardId(String board, String cpu) {
        this.board = board;
        this.cpu = cpu != null ? cpu : "";
    }

    public String getBoard() {
        return board;
    }
    
    public boolean hasCpu() {
        return !cpu.isEmpty();
    }

    public String getCpu() {
        return cpu;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 37 * hash + Objects.hashCode(this.board);
        hash = 37 * hash + Objects.hashCode(this.cpu);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final BoardId other = (BoardId) obj;
        if (!Objects.equals(this.board, other.board)) {
            return false;
        }
        if (!Objects.equals(this.cpu, other.cpu)) {
            return false;
        }
        return true;
    }

    

    @Override
    public String toString() {
        return ( cpu != null && !cpu.isEmpty() ) ? board + "_" + cpu : board;
    }
    
    
}
