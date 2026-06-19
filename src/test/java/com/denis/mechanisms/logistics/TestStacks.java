package com.denis.mechanisms.logistics;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

final class TestStacks {
    private TestStacks() {
    }

    static ItemStack stack(Material material) {
        return stack(material, 1, "");
    }

    static ItemStack stack(Material material, int amount) {
        return stack(material, amount, "");
    }

    static ItemStack stack(Material material, int amount, String tag) {
        ItemStack stack = mock(ItemStack.class);
        AtomicInteger currentAmount = new AtomicInteger(amount);
        when(stack.getType()).thenReturn(material);
        when(stack.getAmount()).thenAnswer(ignored -> currentAmount.get());
        when(stack.getMaxStackSize()).thenReturn(64);
        doAnswer(invocation -> {
            currentAmount.set(invocation.getArgument(0));
            return null;
        }).when(stack).setAmount(org.mockito.ArgumentMatchers.anyInt());
        when(stack.clone()).thenAnswer(ignored -> stack(material, currentAmount.get(), tag));
        when(stack.isSimilar(any(ItemStack.class))).thenAnswer(invocation -> {
            ItemStack other = invocation.getArgument(0);
            Object otherTag = other.serialize().get("tag");
            return other.getType() == material && tag.equals(otherTag == null ? "" : String.valueOf(otherTag));
        });
        Map<String, Object> serialized = new LinkedHashMap<>();
        serialized.put("type", material.name());
        serialized.put("tag", tag);
        when(stack.serialize()).thenReturn(serialized);
        return stack;
    }
}
