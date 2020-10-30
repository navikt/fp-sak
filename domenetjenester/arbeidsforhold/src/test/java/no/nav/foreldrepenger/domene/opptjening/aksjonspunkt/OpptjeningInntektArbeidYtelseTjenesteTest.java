package no.nav.foreldrepenger.domene.opptjening.aksjonspunkt;

import static no.nav.foreldrepenger.domene.arbeidsforhold.YtelseTestHelper.opprettInntektArbeidYtelseAggregatForYrkesaktivitet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
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
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsgiver.VirksomhetTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.foreldrepenger.domene.iay.modell.OppgittOpptjeningBuilder;
import no.nav.foreldrepenger.domene.iay.modell.Opptjeningsnøkkel;
import no.nav.foreldrepenger.domene.iay.modell.VersjonType;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetBuilder;
import no.nav.foreldrepenger.domene.opptjening.OpptjeningAktivitetPeriode;
import no.nav.foreldrepenger.domene.opptjening.OpptjeningInntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.opptjening.OpptjeningsperioderTjeneste;
import no.nav.foreldrepenger.domene.opptjening.VurderingsStatus;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;

public class OpptjeningInntektArbeidYtelseTjenesteTest extends EntityManagerAwareTest {

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
    void setUp() {
        behandlingRepository = new BehandlingRepository(getEntityManager());
        fagsakRepository = new FagsakRepository(getEntityManager());
        opptjeningRepository = new OpptjeningRepository(getEntityManager(), behandlingRepository);
        var apoOpptjening = new AksjonspunktutlederForVurderOppgittOpptjening(opptjeningRepository, iayTjeneste,
            virksomhetTjeneste);
        var apbOpptjening = new AksjonspunktutlederForVurderBekreftetOpptjening(opptjeningRepository, iayTjeneste);
        var opptjeningsperioderTjeneste = new OpptjeningsperioderTjeneste(iayTjeneste, opptjeningRepository, apoOpptjening,
            apbOpptjening);
        opptjeningTjeneste = new OpptjeningInntektArbeidYtelseTjeneste(iayTjeneste, opptjeningRepository,
            opptjeningsperioderTjeneste);
    }

    @Test
    public void skal_utlede_en_periode_for_egen_næring() {
        // Arrange
        final Behandling behandling = opprettBehandling(skjæringstidspunkt);

        DatoIntervallEntitet periode = DatoIntervallEntitet.fraOgMedTilOgMed(skjæringstidspunkt.minusMonths(3), skjæringstidspunkt.minusMonths(2));

        Virksomhet virksomhet = new Virksomhet.Builder()
            .medOrgnr(NAV_ORG_NUMMER)
            .medNavn("Virksomheten")
            .medRegistrert(LocalDate.now())
            .medOppstart(LocalDate.now())
            .build();
        when(virksomhetTjeneste.finnOrganisasjon(NAV_ORG_NUMMER)).thenReturn(Optional.of(virksomhet));

        OppgittOpptjeningBuilder oppgitt = OppgittOpptjeningBuilder.ny();
        oppgitt.leggTilEgneNæringer(Collections.singletonList(OppgittOpptjeningBuilder.EgenNæringBuilder.ny()
            .medVirksomhet(NAV_ORG_NUMMER)
            .medPeriode(periode)
            .medRegnskapsførerNavn("Børre Larsen")
            .medRegnskapsførerTlf("TELEFON")
            .medBegrunnelse("Hva mer?")));

        iayTjeneste.lagreOppgittOpptjening(behandling.getId(), oppgitt);

        // Assert
        BehandlingReferanse ref = BehandlingReferanse.fra(behandling, skjæringstidspunkt);
        List<OpptjeningAktivitetPeriode> perioder = opptjeningTjeneste.hentRelevanteOpptjeningAktiveterForVilkårVurdering(ref)
            .stream().filter(p -> p.getOpptjeningAktivitetType().equals(OpptjeningAktivitetType.NÆRING)).collect(Collectors.toList());

        assertThat(perioder).hasSize(1);
        OpptjeningAktivitetPeriode aktivitetPeriode = perioder.get(0);
        assertThat(aktivitetPeriode.getPeriode()).isEqualTo(periode);
        assertThat(aktivitetPeriode.getVurderingsStatus()).isEqualTo(VurderingsStatus.TIL_VURDERING);
    }

    @Test
    public void skal_sammenstille_grunnlag_og_overstyrt_deretter_utlede_opptjening_aktivitet_periode_for_vilkår_godkjent() {
        // Arrange
        final Behandling behandling = opprettBehandling(skjæringstidspunkt);

        DatoIntervallEntitet periode = DatoIntervallEntitet.fraOgMedTilOgMed(skjæringstidspunkt.minusMonths(3), skjæringstidspunkt.minusMonths(2));


        InntektArbeidYtelseAggregatBuilder bekreftet = opprettInntektArbeidYtelseAggregatForYrkesaktivitet(
            AKTØRID, ARBEIDSFORHOLD_ID, periode, ArbeidType.ORDINÆRT_ARBEIDSFORHOLD, BigDecimal.ZERO, Arbeidsgiver.virksomhet(NAV_ORG_NUMMER),
            VersjonType.REGISTER);
        iayTjeneste.lagreIayAggregat(behandling.getId(), bekreftet);

        // simulerer at det har blitt godkjent i GUI
        InntektArbeidYtelseAggregatBuilder saksbehandling = opprettInntektArbeidYtelseAggregatForYrkesaktivitet(
            AKTØRID, ARBEIDSFORHOLD_ID, periode, ArbeidType.ORDINÆRT_ARBEIDSFORHOLD, BigDecimal.ZERO, Arbeidsgiver.virksomhet(NAV_ORG_NUMMER),
            VersjonType.SAKSBEHANDLET);
        iayTjeneste.lagreIayAggregat(behandling.getId(), saksbehandling);

        // Act
        BehandlingReferanse ref = BehandlingReferanse.fra(behandling, skjæringstidspunkt);
        List<OpptjeningAktivitetPeriode> perioder = opptjeningTjeneste.hentRelevanteOpptjeningAktiveterForVilkårVurdering(ref);
        assertThat(perioder).hasSize(1);
        assertThat(perioder.stream().filter(p -> p.getVurderingsStatus().equals(VurderingsStatus.FERDIG_VURDERT_GODKJENT)).collect(Collectors.toList()))
            .hasSize(1);
    }

    @Test
    public void skal_sammenstille_grunnlag_og_overstyrt_deretter_utlede_opptjening_aktivitet_periode_for_vilkår_underkjent() {
        // Arrange
        LocalDate iDag = LocalDate.now();
        final Behandling behandling = opprettBehandling(iDag);

        DatoIntervallEntitet periode1 = DatoIntervallEntitet.fraOgMedTilOgMed(iDag.minusMonths(3), iDag.minusMonths(2));

        final Arbeidsgiver virksomhet = Arbeidsgiver.virksomhet(NAV_ORG_NUMMER);
        InntektArbeidYtelseAggregatBuilder bekreftet = opprettInntektArbeidYtelseAggregatForYrkesaktivitet(AKTØRID, ARBEIDSFORHOLD_ID, periode1,
            ArbeidType.ORDINÆRT_ARBEIDSFORHOLD, BigDecimal.ZERO, virksomhet, VersjonType.REGISTER);
        iayTjeneste.lagreIayAggregat(behandling.getId(), bekreftet);

        // simulerer at det har blitt underkjent i GUI
        InntektArbeidYtelseAggregatBuilder overstyrt = InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.of(bekreftet.build()), VersjonType.SAKSBEHANDLET);

        YrkesaktivitetBuilder yrkesaktivitetBuilder = overstyrt.getAktørArbeidBuilder(AKTØRID)
            .getYrkesaktivitetBuilderForNøkkelAvType(new Opptjeningsnøkkel(ARBEIDSFORHOLD_ID, NAV_ORG_NUMMER, null),
                ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);
        yrkesaktivitetBuilder.tilbakestillAvtaler();
        iayTjeneste.lagreIayAggregat(behandling.getId(), overstyrt);

        // Act
        BehandlingReferanse behandlingReferanse = BehandlingReferanse.fra(behandling, skjæringstidspunkt);
        List<OpptjeningAktivitetPeriode> perioder = opptjeningTjeneste.hentRelevanteOpptjeningAktiveterForVilkårVurdering(behandlingReferanse);
        assertThat(perioder).hasSize(1);
        assertThat(perioder.stream().filter(p -> p.getVurderingsStatus().equals(VurderingsStatus.FERDIG_VURDERT_UNDERKJENT)).collect(Collectors.toList()))
            .hasSize(1);
    }

    private Behandling opprettBehandling(LocalDate iDag) {
        final Fagsak fagsak = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, NavBruker.opprettNyNB(AKTØRID));
        fagsakRepository.opprettNy(fagsak);
        final Behandling.Builder builder = Behandling.forFørstegangssøknad(fagsak);
        final Behandling behandling = builder.build();
        Behandlingsresultat.opprettFor(behandling);
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
        final VilkårResultat nyttResultat = VilkårResultat.builder().buildFor(behandling);
        behandlingRepository.lagre(nyttResultat, behandlingRepository.taSkriveLås(behandling));

        opptjeningRepository.lagreOpptjeningsperiode(behandling, skjæringstidspunkt.minusMonths(10), skjæringstidspunkt, false);
        return behandling;
    }
}
