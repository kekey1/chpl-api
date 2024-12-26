package gov.healthit.chpl.scheduler.job.sed;

import java.util.EnumMap;
import java.util.Map;

import lombok.Getter;

public class CategoryStats {
    public enum Category {
        RECORD,
        CHANGE,
        ACCESS,
        MULTIPLE,
        UNKNOWN;

        public String toLowerCase() {
            return this.name().toLowerCase();
        }
    }

    @Getter
    private final Map<Category, Integer> categories;
    private int total = 0;

    public CategoryStats() {
        categories = new EnumMap<>(Category.class);
        for (Category category : Category.values()) {
            categories.put(category, 0);
        }
    }

    public void increment(String category) {
        try {
            Category cat = Category.valueOf(category.toUpperCase());
            categories.put(cat, categories.get(cat) + 1);
            total++;
        } catch (IllegalArgumentException e) {
            // Invalid category provided, ignore
        }
    }

    public String getSummary() {
        StringBuilder summary = new StringBuilder("Category Distribution:\n");
        for (Map.Entry<Category, Integer> entry : categories.entrySet()) {
            double percentage = total > 0 ? (entry.getValue() * 100.0) / total : 0;
            summary.append(String.format("%s: %d (%.1f%%)\n",
                entry.getKey().toLowerCase(),
                entry.getValue(),
                percentage));
        }
        return summary.toString();
    }

    public int getTotal() {
        return total;
    }
}

