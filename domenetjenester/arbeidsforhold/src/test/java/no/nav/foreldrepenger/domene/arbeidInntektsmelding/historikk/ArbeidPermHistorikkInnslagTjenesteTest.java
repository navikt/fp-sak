package no.nav.foreldrepenger.domene.arbeidInntektsmelding.historikk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;
import no.nav.foreldrepenger.dokumentarkiv.DokumentArkivTjeneste;
import no.nav.foreldrepenger.domene.arbeidInntektsmelding.AvklarPermisjonUtenSluttdatoDto;
import no.nav.foreldrepenger.domene.arbeidsforhold.testutilities.behandling.IAYRepositoryProvider;
import no.nav.foreldrepenger.domene.arbeidsgiver.ArbeidsgiverOpplysninger;
import no.nav.foreldrepenger.domene.arbeidsgiver.ArbeidsgiverTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.BekreftetPermisjonStatus;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;


@CdiDbAwareTest
class ArbeidPermHistorikkInnslagTjenesteTest {
    private static final String KUNSTIG_ORG = OrgNummer.KUNSTIG_ORG;
    private static final String NAV_ORGNR = "889640782";
    private static final String INTERN_ARBEIDSFORHOLD_ID = "a6ea6724-868f-11e9-bc42-526af7764f64";
    private static final String INTERN_ARBEIDSFORHOLD_ID_2 = "a6ea6724-868f-11e9-bc42-526af7764f65";
    private static final String ARB_NAVN = "Arbeidsgivernavn";

    @Inject
    private HistorikkTjenesteAdapter historikkAdapter;
    @Inject
    private IAYRepositoryProvider provider;

    @Mock
    private ArbeidsgiverTjeneste arbeidsgiverTjeneste;
    @Mock
    private HistorikkRepository historikkRepository;

    private ArbeidPermHistorikkInnslagTjeneste arbeidPermHistorikkInnslagTjenesteTest;

    @BeforeEach
    void setUp(EntityManager entityManager) {
        provider = new IAYRepositoryProvider(entityManager);
        historikkRepository = new HistorikkRepository(entityManager);
        historikkAdapter = new HistorikkTjenesteAdapter(historikkRepository, mock(DokumentArkivTjeneste.class), provider.getBehandlingRepository());

        new ArbeidsgiverOpplysninger(KUNSTIG_ORG, INTERN_ARBEIDSFORHOLD_ID);
        when(arbeidsgiverTjeneste.hent(any())).thenReturn(new ArbeidsgiverOpplysninger(KUNSTIG_ORG, ARB_NAVN));

        arbeidPermHistorikkInnslagTjenesteTest = new ArbeidPermHistorikkInnslagTjeneste(historikkAdapter, arbeidsgiverTjeneste);
    }

    @Test
    void lagTekstMedArbeidsgiver() {
        var avklarteArbForhold = List.of(
            new AvklarPermisjonUtenSluttdatoDto(KUNSTIG_ORG, INTERN_ARBEIDSFORHOLD_ID, BekreftetPermisjonStatus.BRUK_PERMISJON),
            new AvklarPermisjonUtenSluttdatoDto(NAV_ORGNR, INTERN_ARBEIDSFORHOLD_ID_2, BekreftetPermisjonStatus.IKKE_BRUK_PERMISJON));

        arbeidPermHistorikkInnslagTjenesteTest.opprettHistorikkinnslag(avklarteArbForhold, "begrunnelse");

        assertThat(historikkAdapter.tekstBuilder().getHistorikkinnslagDeler()).hasSize(3);
        assertThat(historikkAdapter.tekstBuilder().getHistorikkinnslagDeler().get(2).getBegrunnelse()).contains("begrunnelse");
    }
}
