package no.nav.foreldrepenger.domene.arbeidInntektsmelding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;
import no.nav.foreldrepenger.dokumentarkiv.DokumentArkivTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.testutilities.behandling.IAYRepositoryProvider;
import no.nav.foreldrepenger.domene.arbeidsgiver.ArbeidsgiverOpplysninger;
import no.nav.foreldrepenger.domene.arbeidsgiver.ArbeidsgiverTjeneste;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;


@CdiDbAwareTest
class ArbeidsforholdHistorikkTjenesteTest {
    private static final String KUNSTIG_ORG = OrgNummer.KUNSTIG_ORG;
    private static final String INTERN_ARBEIDSFORHOLD_ID = "a6ea6724-868f-11e9-bc42-526af7764f64";
    private static final String ARB_NAVN = "Arbeidsgivernavn";
    private static final Arbeidsgiver arbeidsgiver = Arbeidsgiver.virksomhet(KUNSTIG_ORG);
    private static final InternArbeidsforholdRef internArbeidsforholdRef = InternArbeidsforholdRef.ref(INTERN_ARBEIDSFORHOLD_ID);

    @Inject
    private HistorikkTjenesteAdapter historikkAdapter;
    @Inject
    private IAYRepositoryProvider provider;

    @Mock
    private ArbeidsgiverTjeneste arbeidsgiverTjeneste;
    @Mock
    private HistorikkRepository historikkRepository;

    private ArbeidsforholdHistorikkTjeneste arbeidsforholdHistorikkTjeneste;

    @BeforeEach
    void setUp(EntityManager entityManager) {
        provider = new IAYRepositoryProvider(entityManager);
        historikkRepository = new HistorikkRepository(entityManager);
        historikkAdapter = new HistorikkTjenesteAdapter(historikkRepository, mock(DokumentArkivTjeneste.class),
            provider.getBehandlingRepository());

        new ArbeidsgiverOpplysninger(KUNSTIG_ORG, INTERN_ARBEIDSFORHOLD_ID);
        lenient().when(arbeidsgiverTjeneste.hent(arbeidsgiver)).thenReturn(new ArbeidsgiverOpplysninger(KUNSTIG_ORG, ARB_NAVN ));

        arbeidsforholdHistorikkTjeneste = new ArbeidsforholdHistorikkTjeneste(historikkAdapter, arbeidsgiverTjeneste );
    }

    @Test
    void lagTekstMedArbeidsgiver() {
        var tekst = arbeidsforholdHistorikkTjeneste.lagTekstMedArbeidsgiverOgArbeidforholdRef(arbeidsgiver, null);
        assertThat(tekst).isEqualTo(ARB_NAVN + " " +"(" + KUNSTIG_ORG + ")");
    }

    @Test
    void lagTekstMedArbeidsgiverRef() {
        var tekst = arbeidsforholdHistorikkTjeneste.lagTekstMedArbeidsgiverOgArbeidforholdRef(arbeidsgiver, internArbeidsforholdRef);
        assertThat(tekst).isEqualTo(" ...4f64");
    }
}
