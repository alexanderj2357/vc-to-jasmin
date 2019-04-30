package VC.Checker;

import VC.ASTs.Decl;

public final class SymbolTable {

    private int level;
    private IdEntry latest;

    public SymbolTable() {
        level = 1;
        latest = null;
    }

    public void openScope() {
        level++;
    }

    public void closeScope() {

        IdEntry entry;

        entry = this.latest;
        while (entry.level == this.level)
            entry = entry.previousEntry;
        this.level--;
        this.latest = entry;
    }

    public void insert(String id, Decl attr) {

        IdEntry entry;
        entry = new IdEntry(id, attr, this.level, this.latest);
        this.latest = entry;
    }

    public Decl retrieve(String id) {

        IdEntry entry;
        Decl attr = null;
        boolean present = false, searching = true;

        entry = this.latest;
        while (searching) {
            if (entry == null)
                searching = false;
            else if (entry.id.equals(id)) {
                present = true;
                searching = false;
                attr = entry.attr;
            } else
                entry = entry.previousEntry;
        }
        return attr;
    }

    public IdEntry retrieveOneLevel(String id) {
        IdEntry entry;

        entry = this.latest;

        while (entry != null) {
            if (entry.level != this.level)
                return null;
            if (entry.id.equals(id))
                break;
            entry = entry.previousEntry;
        }

        return entry;
    }

}
