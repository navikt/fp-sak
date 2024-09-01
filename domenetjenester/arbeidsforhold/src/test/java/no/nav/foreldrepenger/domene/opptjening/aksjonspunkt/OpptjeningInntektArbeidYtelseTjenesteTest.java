package no.nav.foreldrepenger.domene.opptjening.aksjonspunkt;

import static no.nav.foreldrepenger.domene.arbeidsforhold.YtelseTestHelper.opprettInntektArbeidYtelseAggregatForYrkesaktivitet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Virksomhet;
import no.nav.foreldrepenger.dbstoette.JpaExtension;
import no.nav.foreldrepenger.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsgiver.VirksomhetTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.foreldrepenger.domene.iay.modell.OppgittOpptjeningBuilder;
import no.nav.foreldrepenger.domene.iay.modell.Opptjeningsnøkkel;
import no.nav.foreldrepenger.domene.iay.modell.VersjonType;
import no.nav.foreldrepenger.domene.opptjening.OpptjeningInntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.opptjening.OpptjeningsperioderTjeneste;
import no.nav.foreldrepenger.domene.opptjening.VurderingsStatus;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;

@ExtendWith(JpaExtension.class)
class OpptjeningInntektArbeidYtelseTjenesteTest {

    public static final String NAV_ORG_NUMMER = "889640782";

    private final LocalDate skjæringstidspunkt = LocalDate.now();
    private BehandlingRepository behandlingRepository;
    private FagsakRepository fagsakRepository;
    private final InntektArbeidYtelseTjeneste iayTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();
    private OpptjeningRepository opptjeningRepository;
    private final VirksomhetTjeneste virksomhetTjeneste = mock(VirksomhetTjeneste.class);
    private OpptjeningInntektArbeidYtelseTjeneste opptjeningTjeneste;
    private final InternArbeidsforholdRef ARBEIDSFORHOLD_ID = InternArbeidsforholdRef.nyRef();
    private final AktørId AKTØRID = AktørId.dummy();

    @BeforeEach
    void setUp(EntityManager entityManager) {
        behandlingRepository = new BehandlingRepository(entityManager);
        fagsakRepository = new FagsakRepository(entityManager);
        opptjeningRepository = new OpptjeningRepository(entityManager, behandlingRepository);
        var apoOpptjening = new AksjonspunktutlederForVurderOppgittOpptjening(opptjeningRepository, iayTjeneste,
                virksomhetTjeneste);
        var apbOpptjening = new AksjonspunktutlederForVurderBekreftetOpptjening(opptjeningRepository, iayTjeneste);
        var opptjeningsperioderTjeneste = new OpptjeningsperioderTjeneste(iayTjeneste, opptjeningRepository, apoOpptjening,
                apbOpptjening);
        opptjeningTjeneste = new OpptjeningInntektArbeidYtelseTjeneste(iayTjeneste, opptjeningRepository,
                opptjeningsperioderTjeneste);
    }

    @Test
    void skal_utlede_en_periode_for_egen_næring() {
        // Arrange
        var behandling = opprettBehandling();

        var periode = DatoIntervallEntitet.fraOgMedTilOgMed(skjæringstidspunkt.minusMonths(3), skjæringstidspunkt.minusMonths(2));

        var virksomhet = new Virksomhet.Builder()
                .medOrgnr(NAV_ORG_NUMMER)
                .medNavn("Virksomheten")
                .medRegistrert(LocalDate.now())
                .build();
        when(virksomhetTjeneste.finnOrganisasjon(NAV_ORG_NUMMER)).thenReturn(Optional.of(virksomhet));

        var oppgitt = OppgittOpptjeningBuilder.ny();
        oppgitt.leggTilEgenNæring(Collections.singletonList(OppgittOpptjeningBuilder.EgenNæringBuilder.ny()
                .medVirksomhet(NAV_ORG_NUMMER)
                .medPeriode(periode)
                .medRegnskapsførerNavn("Børre Larsen")
                .medRegnskapsførerTlf("TELEFON")
                .medBegrunnelse("Hva mer?")));

        iayTjeneste.lagreOppgittOpptjening(behandling.getId(), oppgitt);

        // Assert
        var ref = BehandlingReferanse.fra(behandling);
        var perioder = opptjeningTjeneste.hentRelevanteOpptjeningAktiveterForVilkårVurdering(ref, medUtledetSkjæringstidspunkt(skjæringstidspunkt))
                .stream().filter(p -> p.getOpptjeningAktivitetType().equals(OpptjeningAktivitetType.NÆRING)).toList();

        assertThat(perioder).hasSize(1);
        var aktivitetPeriode = perioder.get(0);
        assertThat(aktivitetPeriode.getPeriode()).isEqualTo(periode);
        assertThat(aktivitetPeriode.getVurderingsStatus()).isEqualTo(VurderingsStatus.TIL_VURDERING);
    }

    @Test
    void skal_sammenstille_grunnlag_og_overstyrt_deretter_utlede_opptjening_aktivitet_periode_for_vilkår_godkjent() {
        // Arrange
        var behandling = opprettBehandling();

        var periode = DatoIntervallEntitet.fraOgMedTilOgMed(skjæringstidspunkt.minusMonths(3), skjæringstidspunkt.minusMonths(2));

        var bekreftet = opprettInntektArbeidYtelseAggregatForYrkesaktivitet(
                AKTØRID, ARBEIDSFORHOLD_ID, periode, ArbeidType.ORDINÆRT_ARBEIDSFORHOLD, BigDecimal.ZERO, Arbeidsgiver.virksomhet(NAV_ORG_NUMMER),
                VersjonType.REGISTER);
        iayTjeneste.lagreIayAggregat(behandling.getId(), bekreftet);

        // simulerer at det har blitt godkjent i GUI
        var saksbehandling = opprettInntektArbeidYtelseAggregatForYrkesaktivitet(
                AKTØRID, ARBEIDSFORHOLD_ID, periode, ArbeidType.ORDINÆRT_ARBEIDSFORHOLD, BigDecimal.ZERO, Arbeidsgiver.virksomhet(NAV_ORG_NUMMER),
                VersjonType.SAKSBEHANDLET);
        iayTjeneste.lagreIayAggregat(behandling.getId(), saksbehandling);

        // Act
        var ref = BehandlingReferanse.fra(behandling);
        var perioder = opptjeningTjeneste.hentRelevanteOpptjeningAktiveterForVilkårVurdering(ref, medUtledetSkjæringstidspunkt(skjæringstidspunkt));
        assertThat(perioder).hasSize(1);
        assertThat(
                perioder.stream().filter(p -> p.getVurderingsStatus().equals(VurderingsStatus.FERDIG_VURDERT_GODKJENT)).toList())
                        .hasSize(1);
    }

    @Test
    void skal_sammenstille_grunnlag_og_overstyrt_deretter_utlede_opptjening_aktivitet_periode_for_vilkår_underkjent() {
        // Arrange
        var iDag = LocalDate.now();
        var behandling = opprettBehandling();

        var periode1 = DatoIntervallEntitet.fraOgMedTilOgMed(iDag.minusMonths(3), iDag.minusMonths(2));

        var virksomhet = Arbeidsgiver.virksomhet(NAV_ORG_NUMMER);
        var bekreftet = opprettInntektArbeidYtelseAggregatForYrkesaktivitet(AKTØRID, ARBEIDSFORHOLD_ID, periode1,
                ArbeidType.ORDINÆRT_ARBEIDSFORHOLD, BigDecimal.ZERO, virksomhet, VersjonType.REGISTER);
        iayTjeneste.lagreIayAggregat(behandling.getId(), bekreftet);

        // simulerer at det har blitt underkjent i GUI
        var overstyrt = InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.of(bekreftet.build()),
                VersjonType.SAKSBEHANDLET);

        var yrkesaktivitetBuilder = overstyrt.getAktørArbeidBuilder(AKTØRID)
                .getYrkesaktivitetBuilderForNøkkelAvType(new Opptjeningsnøkkel(ARBEIDSFORHOLD_ID, NAV_ORG_NUMMER, null),
                        ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);
        yrkesaktivitetBuilder.tilbakestillAvtaler();
        iayTjeneste.lagreIayAggregat(behandling.getId(), overstyrt);

        // Act
        var behandlingReferanse = BehandlingReferanse.fra(behandling);
        var perioder = opptjeningTjeneste.hentRelevanteOpptjeningAktiveterForVilkårVurdering(behandlingReferanse, medUtledetSkjæringstidspunkt(skjæringstidspunkt));
        assertThat(perioder).hasSize(1);
        assertThat(perioder.stream().filter(p -> p.getVurderingsStatus().equals(VurderingsStatus.FERDIG_VURDERT_UNDERKJENT))
                .toList())
                        .hasSize(1);
    }

    private Behandling opprettBehandling() {
        var fagsak = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, NavBruker.opprettNyNB(AKTØRID));
        fagsakRepository.opprettNy(fagsak);
        var builder = Behandling.forFørstegangssøknad(fagsak);
        var behandling = builder.build();
        Behandlingsresultat.opprettFor(behandling);
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
        var nyttResultat = VilkårResultat.builder().buildFor(behandling);
        behandlingRepository.lagre(nyttResultat, behandlingRepository.taSkriveLås(behandling));

        opptjeningRepository.lagreOpptjeningsperiode(behandling, skjæringstidspunkt.minusMonths(10), skjæringstidspunkt, false);
        return behandling;
    }

    private Skjæringstidspunkt medUtledetSkjæringstidspunkt(LocalDate stp) {
        return Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(stp).build();
    }
}
