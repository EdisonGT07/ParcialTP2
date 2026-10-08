package com.example.demo.dto;

import java.util.ArrayList;
import java.util.List;

public class CompraForm {
    private List<ItemCompra> items = new ArrayList<>();

    public List<ItemCompra> getItems() {
        return items;
    }

    public void setItems(List<ItemCompra> items) {
        this.items = items;
    }
}
