package no.nav.foreldrepenger.økonomistøtte.queue.config;

import java.sql.SQLException;

import jakarta.enterprise.context.ApplicationScoped;

import no.nav.foreldrepenger.felles.jms.precond.PreconditionChecker;
import no.nav.foreldrepenger.felles.jms.precond.PreconditionCheckerResult;
import no.nav.vedtak.felles.jpa.jdbc.DataSourceHolder;

@ApplicationScoped
public class DatabasePreconditionChecker implements PreconditionChecker {

    DatabasePreconditionChecker() {
        // for CDI proxy
    }

    @Override
    public PreconditionCheckerResult check() {
        try (var _ = DataSourceHolder.getDataSource().getConnection()) {
            // Connection pool validerer connections for oss - ikke behov for spørring her (ønsker bare å se om db er tilgjengelig)
            return PreconditionCheckerResult.fullfilled();
        } catch (SQLException e) {
            return PreconditionCheckerResult.notFullfilled(e.getMessage());
        }
    }
}
