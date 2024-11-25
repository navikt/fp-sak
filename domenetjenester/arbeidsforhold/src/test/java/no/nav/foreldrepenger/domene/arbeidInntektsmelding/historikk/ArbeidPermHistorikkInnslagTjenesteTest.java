package no.nav.foreldrepenger.domene.arbeidInntektsmelding.historikk;

import static no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType.FAKTA_OM_ARBEIDSFORHOLD_PERMISJON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag2Repository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.arbeidInntektsmelding.AvklarPermisjonUtenSluttdatoDto;
import no.nav.foreldrepenger.domene.arbeidsgiver.ArbeidsgiverOpplysninger;
import no.nav.foreldrepenger.domene.arbeidsgiver.ArbeidsgiverTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.BekreftetPermisjonStatus;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.historikk.VurderArbeidsforholdHistorikkinnslag;


class ArbeidPermHistorikkInnslagTjenesteTest extends EntityManagerAwareTest {
    private static final String KUNSTIG_ORG = OrgNummer.KUNSTIG_ORG;
    private static final String NAV_ORGNR = "889640782";
    private static final String INTERN_ARBEIDSFORHOLD_ID = "a6ea6724-868f-11e9-bc42-526af7764f64";
    private static final String INTERN_ARBEIDSFORHOLD_ID_2 = "a6ea6724-868f-11e9-bc42-526af7764f65";
    private static final String ARB_NAVN = "Arbeidsgivernavn";

    private ArbeidPermHistorikkInnslagTjeneste arbeidPermHistorikkInnslagTjenesteTest;
    private Historikkinnslag2Repository historikkRepository;

    @BeforeEach
    void setUp() {
        var repositoryProvider = new BehandlingRepositoryProvider(getEntityManager());
        var arbeidsgiverTjeneste = mock(ArbeidsgiverTjeneste.class);
        when(arbeidsgiverTjeneste.hent(any())).thenReturn(new ArbeidsgiverOpplysninger(KUNSTIG_ORG, ARB_NAVN ));

        historikkRepository = repositoryProvider.getHistorikkinnslag2Repository();
        arbeidPermHistorikkInnslagTjenesteTest = new ArbeidPermHistorikkInnslagTjeneste(historikkRepository, arbeidsgiverTjeneste);
    }

    @Test
    void lagTekstMedArbeidsgiver() {
        var avklarteArbForhold = List.of(
            new AvklarPermisjonUtenSluttdatoDto(KUNSTIG_ORG, INTERN_ARBEIDSFORHOLD_ID, BekreftetPermisjonStatus.BRUK_PERMISJON),
            new AvklarPermisjonUtenSluttdatoDto(NAV_ORGNR, INTERN_ARBEIDSFORHOLD_ID_2, BekreftetPermisjonStatus.IKKE_BRUK_PERMISJON)
        );
        var ref = getBehandlingReferanse();
        arbeidPermHistorikkInnslagTjenesteTest.opprettHistorikkinnslag(ref, avklarteArbForhold, "begrunnelse");

        var historikkinnslag = historikkRepository.hent(ref.behandlingId());

        assertThat(historikkinnslag).hasSize(1);
        assertThat(historikkinnslag.getFirst().getFagsakId()).isEqualTo(ref.fagsakId());
        assertThat(historikkinnslag.getFirst().getBehandlingId()).isEqualTo(ref.behandlingId());
        assertThat(historikkinnslag.getFirst().getSkjermlenke()).isEqualTo(FAKTA_OM_ARBEIDSFORHOLD_PERMISJON);
        assertThat(historikkinnslag.getFirst().getTekstlinjer()).hasSize(3);
        assertThat(historikkinnslag.getFirst().getTekstlinjer().get(0).getTekst()).contains(ARB_NAVN, VurderArbeidsforholdHistorikkinnslag.SØKER_ER_I_PERMISJON.getNavn());
        assertThat(historikkinnslag.getFirst().getTekstlinjer().get(1).getTekst()).contains(ARB_NAVN, VurderArbeidsforholdHistorikkinnslag.SØKER_ER_IKKE_I_PERMISJON.getNavn());
        assertThat(historikkinnslag.getFirst().getTekstlinjer().get(1).getTekst()).contains("begrunnelse");
    }

    private static BehandlingReferanse getBehandlingReferanse() {
        return new BehandlingReferanse(
            new Saksnummer("1234456"),
            1000001L,
            FagsakYtelseType.FORELDREPENGER,
            1000001L,
            null,
            BehandlingStatus.OPPRETTET,
            null,
            null,
            AktørId.dummy(),
            null
        );
    }
}
