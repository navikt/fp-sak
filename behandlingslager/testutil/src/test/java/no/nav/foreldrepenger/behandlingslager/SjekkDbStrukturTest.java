package no.nav.foreldrepenger.behandlingslager;

import java.util.List;

import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.foreldrepenger.dbstoette.JpaExtension;
import no.nav.vedtak.felles.testutilities.db.AbstractOracleDbStrukturTest;

/**
 * Tester at alle migreringer følger standarder for navn og god praksis.
 */
@ExtendWith(JpaExtension.class)
class SjekkDbStrukturTest extends AbstractOracleDbStrukturTest {

    @Override
    protected List<String> ekskluderteTabellmønstre() {
        // TODO TFP-7163: Rett PK-/indeksnavn, indekser GR_EOS_UTTAK.SAKSBEHANDLER_PERIODER_ID og fjern unntakene.
        return List.of("EOS_UTTAKSPERIODE", "EOS_UTTAKSPERIODER", "GR_EOS_UTTAK", "BEHANDLING_MELLOMLAGRING");
    }
}
