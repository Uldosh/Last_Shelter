package utility;

public class CheckpointSystem {
    private GameSnapshot checkpoint;

    public void mark(GameSnapshot snapshot) {
        checkpoint = snapshot;
    }

    public boolean hasCheckpoint() {
        return checkpoint != null;
    }

    public GameSnapshot restore() {
        return chec;
    }
}
