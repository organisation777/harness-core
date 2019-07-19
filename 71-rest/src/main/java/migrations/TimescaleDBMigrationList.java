package migrations;

import com.google.common.collect.ImmutableList;

import migrations.timescaledb.ChangeToTimeStampTZ;
import migrations.timescaledb.DeploymentAdditionalColumns;
import migrations.timescaledb.InitSchemaMigration;
import migrations.timescaledb.InitVerificationSchemaMigration;
import migrations.timescaledb.RenameInstanceMigration;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

public class TimescaleDBMigrationList {
  public static List<Pair<Integer, Class<? extends TimeScaleDBMigration>>> getMigrations() {
    return new ImmutableList.Builder<Pair<Integer, Class<? extends TimeScaleDBMigration>>>()
        .add(Pair.of(1, InitSchemaMigration.class))
        .add(Pair.of(2, InitVerificationSchemaMigration.class))
        .add(Pair.of(3, RenameInstanceMigration.class))
        .add(Pair.of(4, DeploymentAdditionalColumns.class))
        .add(Pair.of(5, ChangeToTimeStampTZ.class))
        .build();
  }
}
