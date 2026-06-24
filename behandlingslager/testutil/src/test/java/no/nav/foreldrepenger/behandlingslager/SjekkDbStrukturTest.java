package no.nav.foreldrepenger.behandlingslager;

import java.util.List;

import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.foreldrepenger.dbstoette.JpaExtension;
import no.nav.vedtak.felles.jpa.NamingStandard;
import no.nav.vedtak.felles.testutilities.db.AbstractOracleDbStrukturTest;

/**
 * Tester at alle migreringer følger standarder for navn og god praksis.
 */
@ExtendWith(JpaExtension.class)
class SjekkDbStrukturTest extends AbstractOracleDbStrukturTest {

    @Override
    protected String getOwner() {
        return NamingStandard.DEFAULT_DATA_SOURCE;
    }

    @Override
    protected List<String> ekskluderteTabellmønstre() {
        return List.of("%_MOCK", "HTE_%");
    }

    @Override
    protected List<String> ekskluderteKolonner() {
        return List.of("LANDKODE");
    }
}
