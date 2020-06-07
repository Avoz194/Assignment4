public class CuckooLog {
    /**
     * Class to create Log entities for changes in the array during insert actions.
     *
     */
    private String value;
    private int preIndex;//index of the value before the change

    public CuckooLog(String value, int preIndex) {
        this.preIndex = preIndex;
        this.value = value;
    }

    public int getPreIndex() {
        return preIndex;
    }

    public String getValue() {
        return value;
    }

}