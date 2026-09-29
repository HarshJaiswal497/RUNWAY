package com.runway.gateway.type;

public enum Role {
    ADMIN(4),
    SELLER(3),
    SUPPORT(2),
    CUSTOMER(1);

    private final int rank;

    Role(int rank) {
        this.rank = rank;
    }

    public int getRank() {
        return rank;
    }
}

