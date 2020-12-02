package no.nav.foreldrepenger.domene.opptjening.aksjonspunkt;

import static no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer.KUNSTIG_ORG;
import static no.nav.foreldrepenger.domene.arbeidsforhold.YtelseTestHelper.leggTilYtelseMedAnvist;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.ytelse.RelatertYtelseType;
import no.nav.foreldrepenger.dbstoette.FPsakEntityManagerAwareExtension;
import no.nav.foreldrepenger.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsgiver.VirksomhetTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.AktivitetsAvtaleBuilder;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdInformasjonBuilder;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdOverstyringBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.InntektBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektspostBuilder;
import no.nav.foreldrepenger.domene.iay.modell.OppgittAnnenAktivitet;
import no.nav.foreldrepenger.domene.iay.modell.OppgittOpptjeningBuilder;
import no.nav.foreldrepenger.domene.iay.modell.Opptjeningsnøkkel;
import no.nav.foreldrepenger.domene.iay.modell.Permisjon;
import no.nav.foreldrepenger.domene.iay.modell.PermisjonBuilder;
import no.nav.foreldrepenger.domene.iay.modell.VersjonType;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetBuilder;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.ArbeidsforholdHandlingType;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.InntektsKilde;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.InntektspostType;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.PermisjonsbeskrivelseType;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.RelatertYtelseTilstand;
import no.nav.foreldrepenger.domene.opptjening.OpptjeningsperiodeForSaksbehandling;
import no.nav.foreldrepenger.domene.opptjening.OpptjeningsperioderTjeneste;
import no.nav.foreldrepenger.domene.opptjening.VurderingsStatus;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.tid.VirkedagUtil;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.EksternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.typer.Stillingsprosent;

@ExtendWith(FPsakEntityManagerAwareExtension.class)
public class OpptjeningsperioderTjenesteTest {

    private static final String ORG_NUMMER = KUNSTIG_ORG;

    private BehandlingRepository behandlingRepository;

    private FagsakRepository fagsakRepository;

    private OpptjeningRepository opptjeningRepository;

    private final InntektArbeidYtelseTjeneste iayTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();
    private final VirksomhetTjeneste virksomhetTjeneste = mock(VirksomhetTjeneste.class);
    private OpptjeningsperioderTjeneste forSaksbehandlingTjeneste;

    private final InternArbeidsforholdRef ARBEIDSFORHOLD_ID = InternArbeidsforholdRef.nyRef();
    private final AktørId AKTØRID = AktørId.dummy();
    private final LocalDate skjæringstidspunkt = LocalDate.now();

    @BeforeEach
    void setUp(EntityManager entityManager) {
        behandlingRepository = new BehandlingRepository(entityManager);
        fagsakRepository = new FagsakRepository(entityManager);
        opptjeningRepository = new OpptjeningRepository(entityManager, behandlingRepository);
        var aksjonspunktutlederForVurderOpptjening = new AksjonspunktutlederForVurderOppgittOpptjening(opptjeningRepository,
                iayTjeneste, virksomhetTjeneste);
        var apbOpptjening = new AksjonspunktutlederForVurderBekreftetOpptjening(opptjeningRepository, iayTjeneste);
        forSaksbehandlingTjeneste = new OpptjeningsperioderTjeneste(iayTjeneste, opptjeningRepository, aksjonspunktutlederForVurderOpptjening,
                apbOpptjening);
    }

    @Test
    public void skal_utlede_opptjening_aktivitet_periode_uten_overstyrt() {
        // Arrange
        final Behandling behandling = opprettBehandling();

        DatoIntervallEntitet periode1 = DatoIntervallEntitet.fraOgMedTilOgMed(skjæringstidspunkt.minusMonths(3), skjæringstidspunkt.minusMonths(2));
        DatoIntervallEntitet periode2 = DatoIntervallEntitet.fraOgMedTilOgMed(skjæringstidspunkt.minusMonths(2), skjæringstidspunkt.minusMonths(1));

        OppgittOpptjeningBuilder oppgitt = OppgittOpptjeningBuilder.ny();
        oppgitt.leggTilAnnenAktivitet(new OppgittAnnenAktivitet(periode2, ArbeidType.MILITÆR_ELLER_SIVILTJENESTE));
        iayTjeneste.lagreOppgittOpptjening(behandling.getId(), oppgitt);

        final Arbeidsgiver virksomhet = Arbeidsgiver.virksomhet(ORG_NUMMER);
        InntektArbeidYtelseAggregatBuilder bekreftet = opprettInntektArbeidYtelseAggregatForYrkesaktivitet(AKTØRID, ARBEIDSFORHOLD_ID, periode1,
                ArbeidType.ORDINÆRT_ARBEIDSFORHOLD, BigDecimal.TEN, virksomhet);
        iayTjeneste.lagreIayAggregat(behandling.getId(), bekreftet);

        InntektArbeidYtelseAggregatBuilder saksbehandlet = InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.of(bekreftet.build()),
                VersjonType.SAKSBEHANDLET);

        iayTjeneste.lagreIayAggregat(behandling.getId(), saksbehandlet);

        // Act
        BehandlingReferanse behandlingRef = BehandlingReferanse.fra(behandling, skjæringstidspunkt);
        InntektArbeidYtelseGrunnlag iayGrunnlag = iayTjeneste.hentGrunnlag(behandling.getId());
        List<OpptjeningsperiodeForSaksbehandling> perioder = forSaksbehandlingTjeneste
                .hentRelevanteOpptjeningAktiveterForSaksbehandling(behandlingRef, Optional.of(iayGrunnlag));

        // Assert
        assertThat(perioder).hasSize(2);
        OpptjeningsperiodeForSaksbehandling saksbehandletPeriode = perioder.stream().filter(p -> p.getOpptjeningsnøkkel()
                .getArbeidsforholdRef().map(r -> r.gjelderFor(ARBEIDSFORHOLD_ID)).orElse(false)).findFirst().get();
        assertThat(saksbehandletPeriode.getPeriode()).isEqualTo(periode1);
    }

    @Test
    public void ytelse_skal_lage_sammehengende_liste() {
        // Arrange
        final Behandling behandling = opprettBehandling();

        DatoIntervallEntitet periode = DatoIntervallEntitet.fraOgMedTilOgMed(skjæringstidspunkt.minusMonths(3), skjæringstidspunkt);

        final Arbeidsgiver virksomhet = Arbeidsgiver.virksomhet(ORG_NUMMER);
        InntektArbeidYtelseAggregatBuilder bekreftet = opprettInntektArbeidYtelseAggregatForYrkesaktivitet(AKTØRID, ARBEIDSFORHOLD_ID, periode,
                ArbeidType.ORDINÆRT_ARBEIDSFORHOLD, BigDecimal.TEN, virksomhet);
        bekreftet.leggTilAktørYtelse(leggTilYtelseMedAnvist(bekreftet.getAktørYtelseBuilder(AKTØRID),
                VirkedagUtil.fomVirkedag(skjæringstidspunkt.minusDays(10)),
                VirkedagUtil.tomSøndag(skjæringstidspunkt.minusDays(2)), RelatertYtelseTilstand.LØPENDE, "12342234", RelatertYtelseType.SYKEPENGER));
        bekreftet.leggTilAktørYtelse(leggTilYtelseMedAnvist(bekreftet.getAktørYtelseBuilder(AKTØRID),
                VirkedagUtil.fomVirkedag(skjæringstidspunkt.minusDays(25)),
                VirkedagUtil.tomSøndag(skjæringstidspunkt.minusDays(5)), RelatertYtelseTilstand.AVSLUTTET, "1222433", RelatertYtelseType.SYKEPENGER));
        bekreftet.leggTilAktørYtelse(leggTilYtelseMedAnvist(bekreftet.getAktørYtelseBuilder(AKTØRID),
                VirkedagUtil.fomVirkedag(skjæringstidspunkt.minusDays(30)),
                VirkedagUtil.tomSøndag(skjæringstidspunkt.minusDays(21)), RelatertYtelseTilstand.AVSLUTTET, "124234", RelatertYtelseType.SYKEPENGER));
        bekreftet.leggTilAktørYtelse(
                leggTilYtelseMedAnvist(bekreftet.getAktørYtelseBuilder(AKTØRID), VirkedagUtil.fomVirkedag(skjæringstidspunkt.minusDays(50)),
                        VirkedagUtil.tomSøndag(skjæringstidspunkt.minusDays(40)), RelatertYtelseTilstand.AVSLUTTET, "123253254",
                        RelatertYtelseType.SYKEPENGER));

        iayTjeneste.lagreIayAggregat(behandling.getId(), bekreftet);

        opptjeningRepository.lagreOpptjeningsperiode(behandling, skjæringstidspunkt.minusDays(30), skjæringstidspunkt, false);

        // Act
        BehandlingReferanse behandlingRef = BehandlingReferanse.fra(behandling, skjæringstidspunkt);
        InntektArbeidYtelseGrunnlag iayGrunnlag = iayTjeneste.hentGrunnlag(behandling.getId());
        List<OpptjeningsperiodeForSaksbehandling> perioder = forSaksbehandlingTjeneste
                .hentRelevanteOpptjeningAktiveterForSaksbehandling(behandlingRef, Optional.of(iayGrunnlag));

        // Assert
        assertThat(perioder).hasSize(3);
        var ytelser = perioder.stream()
                .filter(p -> OpptjeningAktivitetType.SYKEPENGER.equals(p.getOpptjeningAktivitetType()))
                .collect(Collectors.toList());
        assertThat(ytelser.get(0).getPeriode().getFomDato()).isEqualTo(VirkedagUtil.fomVirkedag(skjæringstidspunkt.minusDays(50)));
        assertThat(ytelser.get(1).getPeriode().getFomDato()).isEqualTo(VirkedagUtil.fomVirkedag(skjæringstidspunkt.minusDays(30)));
        assertThat(ytelser.get(1).getPeriode().getTomDato()).isEqualTo(VirkedagUtil.tomSøndag(skjæringstidspunkt.minusDays(2)));

    }

    @Test
    public void ytelse_skal_ikke_ta_med_ytelser_etter_stp() {
        // Arrange
        final Behandling behandling = opprettBehandling();

        DatoIntervallEntitet periode = DatoIntervallEntitet.fraOgMedTilOgMed(skjæringstidspunkt.minusMonths(3), skjæringstidspunkt);

        final Arbeidsgiver virksomhet = Arbeidsgiver.virksomhet(ORG_NUMMER);
        InntektArbeidYtelseAggregatBuilder bekreftet = opprettInntektArbeidYtelseAggregatForYrkesaktivitet(AKTØRID, ARBEIDSFORHOLD_ID, periode,
                ArbeidType.ORDINÆRT_ARBEIDSFORHOLD, BigDecimal.TEN, virksomhet);
        bekreftet.leggTilAktørYtelse(leggTilYtelseMedAnvist(bekreftet.getAktørYtelseBuilder(AKTØRID),
                VirkedagUtil.fomVirkedag(skjæringstidspunkt.minusDays(15)),
                VirkedagUtil.tomSøndag(skjæringstidspunkt.minusDays(2)), RelatertYtelseTilstand.LØPENDE, "12342234", RelatertYtelseType.SYKEPENGER));
        bekreftet.leggTilAktørYtelse(
                leggTilYtelseMedAnvist(bekreftet.getAktørYtelseBuilder(AKTØRID), VirkedagUtil.fomVirkedag(skjæringstidspunkt.minusDays(30)),
                        VirkedagUtil.tomSøndag(skjæringstidspunkt.minusDays(15)), RelatertYtelseTilstand.AVSLUTTET, "1222433",
                        RelatertYtelseType.OMSORGSPENGER));
        bekreftet.leggTilAktørYtelse(leggTilYtelseMedAnvist(bekreftet.getAktørYtelseBuilder(AKTØRID),
                VirkedagUtil.fomVirkedag(skjæringstidspunkt.plusDays(10)),
                VirkedagUtil.tomSøndag(skjæringstidspunkt.plusDays(21)), RelatertYtelseTilstand.AVSLUTTET, "124234", RelatertYtelseType.SYKEPENGER));

        iayTjeneste.lagreIayAggregat(behandling.getId(), bekreftet);

        opptjeningRepository.lagreOpptjeningsperiode(behandling, skjæringstidspunkt.minusDays(30), skjæringstidspunkt, false);

        // Act
        BehandlingReferanse behandlingRef = BehandlingReferanse.fra(behandling, skjæringstidspunkt);
        InntektArbeidYtelseGrunnlag iayGrunnlag = iayTjeneste.hentGrunnlag(behandling.getId());
        List<OpptjeningsperiodeForSaksbehandling> perioder = forSaksbehandlingTjeneste
                .hentRelevanteOpptjeningAktiveterForSaksbehandling(behandlingRef, Optional.of(iayGrunnlag));

        // Assert
        assertThat(perioder).hasSize(3);
        var ytelserSP = perioder.stream()
                .filter(p -> OpptjeningAktivitetType.SYKEPENGER.equals(p.getOpptjeningAktivitetType()))
                .collect(Collectors.toList());
        assertThat(ytelserSP).hasSize(1);
        assertThat(ytelserSP.get(0).getPeriode().getFomDato()).isEqualTo(VirkedagUtil.fomVirkedag(skjæringstidspunkt.minusDays(15)));
        assertThat(perioder.stream().map(OpptjeningsperiodeForSaksbehandling::getOpptjeningAktivitetType)
                .filter(OpptjeningAktivitetType.OMSORGSPENGER::equals).count()).isEqualTo(1);

    }

    @Test
    public void skal_sammenstille_grunnlag_og_overstyrt_deretter_utlede_opptjening_aktivitet_periode() {
        // Arrange
        final Behandling behandling = opprettBehandling();

        DatoIntervallEntitet periode1 = DatoIntervallEntitet.fraOgMedTilOgMed(skjæringstidspunkt.minusMonths(3), skjæringstidspunkt.minusMonths(2));
        DatoIntervallEntitet periode2 = DatoIntervallEntitet.fraOgMedTilOgMed(skjæringstidspunkt.minusMonths(2), skjæringstidspunkt.minusMonths(1));
        DatoIntervallEntitet periode3 = DatoIntervallEntitet.fraOgMedTilOgMed(skjæringstidspunkt.minusMonths(1), skjæringstidspunkt.minusMonths(0));

        OppgittOpptjeningBuilder oppgitt = OppgittOpptjeningBuilder.ny();
        oppgitt.leggTilAnnenAktivitet(new OppgittAnnenAktivitet(periode2, ArbeidType.MILITÆR_ELLER_SIVILTJENESTE));
        iayTjeneste.lagreOppgittOpptjening(behandling.getId(), oppgitt);

        final Arbeidsgiver virksomhet = Arbeidsgiver.virksomhet(ORG_NUMMER);
        InntektArbeidYtelseAggregatBuilder bekreftet = opprettInntektArbeidYtelseAggregatForYrkesaktivitet(AKTØRID, ARBEIDSFORHOLD_ID, periode1,
                ArbeidType.ORDINÆRT_ARBEIDSFORHOLD, BigDecimal.TEN, virksomhet);
        iayTjeneste.lagreIayAggregat(behandling.getId(), bekreftet);

        InntektArbeidYtelseAggregatBuilder saksbehandlet = InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.of(bekreftet.build()),
                VersjonType.SAKSBEHANDLET);
        var ref = saksbehandlet.medNyInternArbeidsforholdRef(virksomhet, EksternArbeidsforholdRef.ref("1"));

        YrkesaktivitetBuilder yrkesaktivitetBuilder = saksbehandlet.getAktørArbeidBuilder(AKTØRID)
                .getYrkesaktivitetBuilderForNøkkelAvType(new Opptjeningsnøkkel(ref, null, null),
                        ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);
        yrkesaktivitetBuilder
                .leggTilAktivitetsAvtale(yrkesaktivitetBuilder.getAktivitetsAvtaleBuilder()
                        .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(periode3.getFomDato(), periode3.getTomDato()))
                        .medProsentsats(BigDecimal.TEN));

        iayTjeneste.lagreIayAggregat(behandling.getId(), saksbehandlet);

        // Act
        BehandlingReferanse behandlingRef = BehandlingReferanse.fra(behandling, skjæringstidspunkt);
        List<OpptjeningsperiodeForSaksbehandling> perioder = forSaksbehandlingTjeneste
                .hentRelevanteOpptjeningAktiveterForSaksbehandling(behandlingRef);

        // Assert
        assertThat(perioder.stream().filter(p -> p.getVurderingsStatus().equals(VurderingsStatus.GODKJENT)).collect(Collectors.toList())).hasSize(1);
        assertThat(perioder.stream().filter(p -> p.getVurderingsStatus().equals(VurderingsStatus.UNDERKJENT)).collect(Collectors.toList()))
                .hasSize(1);
    }

    @Test
    public void skal_sammenstille_grunnlag_og_utlede_opptjening_aktivitet_periode() {
        // Arrange
        Behandling behandling = opprettBehandling();

        DatoIntervallEntitet periode1 = DatoIntervallEntitet.fraOgMedTilOgMed(skjæringstidspunkt.minusMonths(3), skjæringstidspunkt.minusMonths(2));
        DatoIntervallEntitet periode2 = DatoIntervallEntitet.fraOgMedTilOgMed(skjæringstidspunkt.minusMonths(2), skjæringstidspunkt.minusMonths(1));

        OppgittOpptjeningBuilder oppgitt = OppgittOpptjeningBuilder.ny();

        OppgittOpptjeningBuilder.EgenNæringBuilder egenNæringBuilder = OppgittOpptjeningBuilder.EgenNæringBuilder.ny();
        egenNæringBuilder
                .medRegnskapsførerNavn("Larsen")
                .medRegnskapsførerTlf("TELEFON")
                .medVirksomhet(ORG_NUMMER)
                .medPeriode(periode2);

        oppgitt.leggTilAnnenAktivitet(new OppgittAnnenAktivitet(periode2, ArbeidType.MILITÆR_ELLER_SIVILTJENESTE));
        oppgitt.leggTilEgneNæringer(List.of(egenNæringBuilder));
        iayTjeneste.lagreOppgittOpptjening(behandling.getId(), oppgitt);

        InntektArbeidYtelseAggregatBuilder bekreftet = opprettInntektArbeidYtelseAggregatForYrkesaktivitet(AKTØRID, ARBEIDSFORHOLD_ID, periode1,
                ArbeidType.ORDINÆRT_ARBEIDSFORHOLD, BigDecimal.TEN, Arbeidsgiver.virksomhet(ORG_NUMMER));
        iayTjeneste.lagreIayAggregat(behandling.getId(), bekreftet);

        BehandlingReferanse ref = BehandlingReferanse.fra(behandling, skjæringstidspunkt);
        // Act
        List<OpptjeningsperiodeForSaksbehandling> perioder = forSaksbehandlingTjeneste.hentRelevanteOpptjeningAktiveterForSaksbehandling(ref);

        // Assert
        assertThat(perioder.stream().filter(p -> p.getVurderingsStatus().equals(VurderingsStatus.GODKJENT)).collect(Collectors.toList())).hasSize(2);
        assertThat(perioder.stream().filter(p -> p.getVurderingsStatus().equals(VurderingsStatus.TIL_VURDERING)).collect(Collectors.toList()))
                .hasSize(1);
        assertThat(perioder.stream().filter(OpptjeningsperiodeForSaksbehandling::getErManueltRegistrert).collect(Collectors.toList())).isEmpty();
        assertThat(perioder.stream().filter(o -> !o.getErManueltRegistrert()).collect(Collectors.toList())).hasSize(3);
    }

    @Test
    public void skal_utlede_om_en_periode_er_blitt_endret() {
        // Arrange
        final Behandling behandling = opprettBehandling();

        DatoIntervallEntitet periode1 = DatoIntervallEntitet.fraOgMedTilOgMed(skjæringstidspunkt.minusMonths(3), skjæringstidspunkt.minusMonths(2));
        DatoIntervallEntitet periode2 = DatoIntervallEntitet.fraOgMedTilOgMed(skjæringstidspunkt.minusMonths(2), skjæringstidspunkt.minusMonths(1));

        OppgittOpptjeningBuilder oppgitt = OppgittOpptjeningBuilder.ny();
        oppgitt.leggTilAnnenAktivitet(new OppgittAnnenAktivitet(periode2, ArbeidType.MILITÆR_ELLER_SIVILTJENESTE));
        iayTjeneste.lagreOppgittOpptjening(behandling.getId(), oppgitt);

        InntektArbeidYtelseAggregatBuilder saksbehandlet = opprettOverstyrtOppgittOpptjening(periode1,
                ArbeidType.MILITÆR_ELLER_SIVILTJENESTE, AKTØRID, VersjonType.SAKSBEHANDLET);
        iayTjeneste.lagreIayAggregat(behandling.getId(), saksbehandlet);
        BehandlingReferanse ref = BehandlingReferanse.fra(behandling, skjæringstidspunkt);
        // Act
        // Assert
        List<OpptjeningsperiodeForSaksbehandling> perioder = forSaksbehandlingTjeneste.hentRelevanteOpptjeningAktiveterForSaksbehandling(ref)
                .stream().filter(p -> p.getOpptjeningAktivitetType().equals(OpptjeningAktivitetType.MILITÆR_ELLER_SIVILTJENESTE))
                .collect(Collectors.toList());

        assertThat(perioder).hasSize(1);
        assertThat(perioder.get(0).getErPeriodeEndret()).isTrue();
        assertThat(perioder.get(0).getBegrunnelse()).isNotEmpty();
    }

    @Test
    public void skal_returnere_oat_frilans_ved_bekreftet_frilans() {
        // Arrange
        final Behandling behandling = opprettBehandling();

        final Arbeidsgiver arbeidsgiver = Arbeidsgiver.virksomhet(ORG_NUMMER);

        LocalDate fraOgMed = LocalDate.now().minusMonths(4);
        LocalDate tilOgMed = LocalDate.now().minusMonths(3);
        DatoIntervallEntitet periode1 = DatoIntervallEntitet.fraOgMedTilOgMed(fraOgMed, tilOgMed);

        InntektArbeidYtelseAggregatBuilder bekreftet = opprettInntektArbeidYtelseAggregatForYrkesaktivitet(AKTØRID, ARBEIDSFORHOLD_ID, periode1,
                ArbeidType.FRILANSER_OPPDRAGSTAKER_MED_MER, BigDecimal.TEN, arbeidsgiver);
        opprettInntektForFrilanser(bekreftet, AKTØRID, ARBEIDSFORHOLD_ID, periode1, arbeidsgiver);
        iayTjeneste.lagreIayAggregat(behandling.getId(), bekreftet);

        BehandlingReferanse ref = BehandlingReferanse.fra(behandling, skjæringstidspunkt);
        opptjeningRepository.lagreOpptjeningsperiode(behandling, LocalDate.now().minusMonths(10), LocalDate.now().minusDays(1), false);

        // Act 1
        List<OpptjeningsperiodeForSaksbehandling> perioder = forSaksbehandlingTjeneste.hentRelevanteOpptjeningAktiveterForSaksbehandling(ref);

        // Assert
        assertThat(perioder.stream().filter(p -> p.getOpptjeningAktivitetType().equals(OpptjeningAktivitetType.FRILANS)).collect(Collectors.toList()))
                .hasSize(1);
        assertThat(perioder.stream().filter(p -> p.getVurderingsStatus().equals(VurderingsStatus.TIL_VURDERING)).collect(Collectors.toList()))
                .hasSize(1);
        assertThat(perioder.stream().filter(OpptjeningsperiodeForSaksbehandling::getErManueltRegistrert).collect(Collectors.toList())).isEmpty();
        assertThat(perioder.stream().filter(o -> !o.getErManueltRegistrert()).collect(Collectors.toList())).hasSize(1);

        // Act 2
        InntektArbeidYtelseAggregatBuilder saksbehandlet = opprettOverstyrtOppgittOpptjening(periode1,
                ArbeidType.MILITÆR_ELLER_SIVILTJENESTE, AKTØRID, VersjonType.SAKSBEHANDLET);
        iayTjeneste.lagreIayAggregat(behandling.getId(), saksbehandlet);
        perioder = forSaksbehandlingTjeneste.hentRelevanteOpptjeningAktiveterForSaksbehandling(ref);

        // Assert
        assertThat(perioder.stream().filter(p -> p.getOpptjeningAktivitetType().equals(OpptjeningAktivitetType.FRILANS)).collect(Collectors.toList()))
                .hasSize(1);
        assertThat(perioder.stream().filter(p -> p.getVurderingsStatus().equals(VurderingsStatus.UNDERKJENT)).collect(Collectors.toList()))
                .hasSize(1);
        assertThat(perioder.stream().filter(OpptjeningsperiodeForSaksbehandling::getErManueltRegistrert).collect(Collectors.toList())).hasSize(1);
    }

    @Test
    public void skal_sette_manuelt_behandlet_ved_underkjent_frilans() {
        // Arrange
        final Behandling behandling = opprettBehandling();

        final Arbeidsgiver arbeidsgiver = Arbeidsgiver.virksomhet(ORG_NUMMER);

        LocalDate fraOgMed = LocalDate.now().minusMonths(4);
        LocalDate tilOgMed = LocalDate.now().minusMonths(3);
        DatoIntervallEntitet periode1 = DatoIntervallEntitet.fraOgMedTilOgMed(fraOgMed, tilOgMed);

        InntektArbeidYtelseAggregatBuilder bekreftet = opprettInntektArbeidYtelseAggregatForYrkesaktivitet(AKTØRID, ARBEIDSFORHOLD_ID, periode1,
                ArbeidType.FRILANSER_OPPDRAGSTAKER_MED_MER, BigDecimal.TEN, arbeidsgiver);
        opprettInntektForFrilanser(bekreftet, AKTØRID, ARBEIDSFORHOLD_ID, periode1, arbeidsgiver);
        iayTjeneste.lagreIayAggregat(behandling.getId(), bekreftet);

        OppgittOpptjeningBuilder oppgitt = OppgittOpptjeningBuilder.ny();
        oppgitt.leggTilAnnenAktivitet(new OppgittAnnenAktivitet(periode1, ArbeidType.FRILANSER));
        iayTjeneste.lagreOppgittOpptjening(behandling.getId(), oppgitt);

        BehandlingReferanse ref = BehandlingReferanse.fra(behandling, skjæringstidspunkt);
        opptjeningRepository.lagreOpptjeningsperiode(behandling, LocalDate.now().minusMonths(10), LocalDate.now().minusDays(1), false);

        // Act 1
        List<OpptjeningsperiodeForSaksbehandling> perioder = forSaksbehandlingTjeneste.hentRelevanteOpptjeningAktiveterForSaksbehandling(ref);

        // Assert
        assertThat(perioder.stream().filter(p -> p.getOpptjeningAktivitetType().equals(OpptjeningAktivitetType.FRILANS)).collect(Collectors.toList()))
                .hasSize(1);
        assertThat(perioder.stream().filter(p -> p.getVurderingsStatus().equals(VurderingsStatus.TIL_VURDERING)).collect(Collectors.toList()))
                .hasSize(1);
        assertThat(perioder.stream().filter(OpptjeningsperiodeForSaksbehandling::getErManueltRegistrert).collect(Collectors.toList())).isEmpty();
        assertThat(perioder.stream().filter(o -> !o.getErManueltRegistrert()).collect(Collectors.toList())).hasSize(1);

        // Act 2
        InntektArbeidYtelseAggregatBuilder saksbehandlet = InntektArbeidYtelseAggregatBuilder
                .oppdatere(Optional.empty(), VersjonType.SAKSBEHANDLET);
        iayTjeneste.lagreIayAggregat(behandling.getId(), saksbehandlet);
        perioder = forSaksbehandlingTjeneste.hentRelevanteOpptjeningAktiveterForSaksbehandling(ref);

        // Assert
        assertThat(perioder.stream().filter(p -> p.getOpptjeningAktivitetType().equals(OpptjeningAktivitetType.FRILANS)).collect(Collectors.toList()))
                .hasSize(1);
        assertThat(perioder.stream().filter(p -> p.getVurderingsStatus().equals(VurderingsStatus.UNDERKJENT)).collect(Collectors.toList()))
                .hasSize(1);
        OpptjeningsperiodeForSaksbehandling frilansPeriode = perioder.stream()
                .filter(p -> p.getOpptjeningAktivitetType().equals(OpptjeningAktivitetType.FRILANS)).findFirst().get();
        assertThat(frilansPeriode.erManueltBehandlet()).isTrue();
    }

    @Test
    public void skal_returnere_oat_frilans_ved_bekreftet_frilans_for_vilkår() {
        // Arrange
        final Behandling behandling = opprettBehandling();

        final Arbeidsgiver arbeidsgiver = Arbeidsgiver.virksomhet(ORG_NUMMER);

        LocalDate fraOgMed = LocalDate.now().minusMonths(4);
        LocalDate tilOgMed = LocalDate.now().minusMonths(3);
        DatoIntervallEntitet periode1 = DatoIntervallEntitet.fraOgMedTilOgMed(fraOgMed, tilOgMed);

        InntektArbeidYtelseAggregatBuilder bekreftet = opprettInntektArbeidYtelseAggregatForYrkesaktivitet(AKTØRID, ARBEIDSFORHOLD_ID, periode1,
                ArbeidType.FRILANSER_OPPDRAGSTAKER_MED_MER, BigDecimal.TEN, arbeidsgiver);
        opprettInntektForFrilanser(bekreftet, AKTØRID, ARBEIDSFORHOLD_ID, periode1, arbeidsgiver);
        iayTjeneste.lagreIayAggregat(behandling.getId(), bekreftet);

        BehandlingReferanse ref = BehandlingReferanse.fra(behandling, skjæringstidspunkt);
        opptjeningRepository.lagreOpptjeningsperiode(behandling, LocalDate.now().minusMonths(10), LocalDate.now().minusDays(1), false);

        // Act 1
        List<OpptjeningsperiodeForSaksbehandling> perioder = forSaksbehandlingTjeneste.hentRelevanteOpptjeningAktiveterForVilkårVurdering(ref);

        // Assert
        assertThat(perioder.stream().filter(p -> p.getOpptjeningAktivitetType().equals(OpptjeningAktivitetType.FRILANS)).collect(Collectors.toList()))
                .hasSize(1);
        assertThat(perioder.stream().filter(p -> p.getVurderingsStatus().equals(VurderingsStatus.FERDIG_VURDERT_UNDERKJENT))
                .collect(Collectors.toList()))
                        .hasSize(1);

        // Act 2
        InntektArbeidYtelseAggregatBuilder saksbehandlet = opprettOverstyrtOppgittOpptjening(periode1, ArbeidType.FRILANSER, AKTØRID,
                VersjonType.SAKSBEHANDLET);
        iayTjeneste.lagreIayAggregat(behandling.getId(), saksbehandlet);
        perioder = forSaksbehandlingTjeneste.hentRelevanteOpptjeningAktiveterForVilkårVurdering(ref);

        // Assert
        assertThat(perioder.stream().filter(p -> p.getOpptjeningAktivitetType().equals(OpptjeningAktivitetType.FRILANS)).collect(Collectors.toList()))
                .hasSize(1);
        assertThat(
                perioder.stream().filter(p -> p.getVurderingsStatus().equals(VurderingsStatus.FERDIG_VURDERT_GODKJENT)).collect(Collectors.toList()))
                        .hasSize(1);
    }

    @Test
    public void skal_returnere_en_periode_med_fiktivt_bekreftet_arbeidsforhold() {
        // Arrange
        final Behandling behandling = opprettBehandling();
        BehandlingReferanse ref = BehandlingReferanse.fra(behandling, skjæringstidspunkt);
        LocalDate fraOgMed = LocalDate.of(2015, 1, 4);
        LocalDate tilOgMed = skjæringstidspunkt.plusMonths(2);
        DatoIntervallEntitet periode = DatoIntervallEntitet.fraOgMedTilOgMed(fraOgMed, tilOgMed);
        InntektArbeidYtelseAggregatBuilder saksbehandlet = lagFiktivtArbeidsforholdSaksbehandlet(periode);
        iayTjeneste.lagreIayAggregat(behandling.getId(), saksbehandlet);
        ArbeidsforholdInformasjonBuilder informasjon = lagFiktivtArbeidsforholdOverstyring(fraOgMed, tilOgMed);
        iayTjeneste.lagreArbeidsforhold(behandling.getId(), AKTØRID, informasjon);

        // Act
        List<OpptjeningsperiodeForSaksbehandling> perioder = forSaksbehandlingTjeneste.hentRelevanteOpptjeningAktiveterForSaksbehandling(ref);

        // Assert
        assertThat(perioder).hasSize(1);
    }

    @Test
    public void skal_kunne_bygge_opptjeninsperiode_basert_på_arbeidsforhold_lagt_til_avsaksbehandler() {
        final Behandling behandling = opprettBehandling();
        BehandlingReferanse ref = BehandlingReferanse.fra(behandling, skjæringstidspunkt);
        LocalDate start = LocalDate.now().minusMonths(5);

        ArbeidsforholdInformasjonBuilder arbeidsforholdInformasjonBuilder = ArbeidsforholdInformasjonBuilder.builder(Optional.empty());
        Arbeidsgiver virksomhet = Arbeidsgiver.virksomhet("999999999");

        ArbeidsforholdOverstyringBuilder arbeidsforholdOverstyringBuilder = arbeidsforholdInformasjonBuilder.getOverstyringBuilderFor(virksomhet,
                null);
        arbeidsforholdOverstyringBuilder.medHandling(ArbeidsforholdHandlingType.BASERT_PÅ_INNTEKTSMELDING);
        arbeidsforholdOverstyringBuilder.leggTilOverstyrtPeriode(start, LocalDate.MAX);
        arbeidsforholdOverstyringBuilder.medAngittStillingsprosent(Stillingsprosent.ZERO);
        arbeidsforholdInformasjonBuilder.leggTil(arbeidsforholdOverstyringBuilder);

        iayTjeneste.lagreArbeidsforhold(behandling.getId(), AKTØRID, arbeidsforholdInformasjonBuilder);

        forSaksbehandlingTjeneste.hentRelevanteOpptjeningAktiveterForSaksbehandling(ref);

    }

    private ArbeidsforholdInformasjonBuilder lagFiktivtArbeidsforholdOverstyring(LocalDate fraOgMed, LocalDate tilOgMed) {
        Arbeidsgiver arbeidsgiver = Arbeidsgiver.virksomhet(KUNSTIG_ORG);
        return ArbeidsforholdInformasjonBuilder.oppdatere(Optional.empty())
                .leggTil(ArbeidsforholdOverstyringBuilder.oppdatere(Optional.empty())
                        .medArbeidsgiver(arbeidsgiver)
                        .medHandling(ArbeidsforholdHandlingType.LAGT_TIL_AV_SAKSBEHANDLER)
                        .leggTilOverstyrtPeriode(fraOgMed, tilOgMed)
                        .medAngittStillingsprosent(new Stillingsprosent(BigDecimal.valueOf(100))));
    }

    private InntektArbeidYtelseAggregatBuilder lagFiktivtArbeidsforholdSaksbehandlet(DatoIntervallEntitet periode) {
        Arbeidsgiver arbeidsgiver = Arbeidsgiver.virksomhet(KUNSTIG_ORG);
        InntektArbeidYtelseAggregatBuilder builder = InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.empty(), VersjonType.SAKSBEHANDLET);
        InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder aktørArbeidBuilder = builder.getAktørArbeidBuilder(AKTØRID);
        YrkesaktivitetBuilder yrkesaktivitetBuilder = aktørArbeidBuilder.getYrkesaktivitetBuilderForNøkkelAvType(
                new Opptjeningsnøkkel(null, KUNSTIG_ORG, null), ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);
        AktivitetsAvtaleBuilder aktivitetsAvtaleBuilder = yrkesaktivitetBuilder.getAktivitetsAvtaleBuilder();
        AktivitetsAvtaleBuilder aktivitetsAvtale = aktivitetsAvtaleBuilder
                .medPeriode(periode)
                .medProsentsats(BigDecimal.valueOf(100))
                .medBeskrivelse("Ser greit ut");
        AktivitetsAvtaleBuilder ansettelsesperiode = yrkesaktivitetBuilder.getAktivitetsAvtaleBuilder()
                .medPeriode(periode);
        yrkesaktivitetBuilder
                .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
                .medArbeidsgiver(arbeidsgiver)
                .medArbeidsforholdId(null)
                .leggTilAktivitetsAvtale(aktivitetsAvtale)
                .leggTilAktivitetsAvtale(ansettelsesperiode);
        InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder aktørArbeid = aktørArbeidBuilder
                .leggTilYrkesaktivitet(yrkesaktivitetBuilder);
        builder.leggTilAktørArbeid(aktørArbeid);
        return builder;
    }

    private InntektArbeidYtelseAggregatBuilder opprettOverstyrtOppgittOpptjening(DatoIntervallEntitet periode, ArbeidType type, AktørId aktørId,
            VersjonType register) {
        InntektArbeidYtelseAggregatBuilder builder = InntektArbeidYtelseAggregatBuilder
                .oppdatere(Optional.empty(), register);

        InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder aktørArbeidBuilder = builder.getAktørArbeidBuilder(aktørId);
        YrkesaktivitetBuilder yrkesaktivitetBuilder = aktørArbeidBuilder.getYrkesaktivitetBuilderForType(type);

        AktivitetsAvtaleBuilder aktivitetsAvtaleBuilder = yrkesaktivitetBuilder.getAktivitetsAvtaleBuilder();

        AktivitetsAvtaleBuilder aktivitetsAvtale = aktivitetsAvtaleBuilder
                .medPeriode(periode)
                .medBeskrivelse("Ser greit ut");

        yrkesaktivitetBuilder
                .medArbeidType(type)
                .leggTilAktivitetsAvtale(aktivitetsAvtale);
        InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder aktørArbeid = aktørArbeidBuilder
                .leggTilYrkesaktivitet(yrkesaktivitetBuilder);

        builder.leggTilAktørArbeid(aktørArbeid);

        return builder;
    }

    private Behandling opprettBehandling() {
        final Fagsak fagsak = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, NavBruker.opprettNyNB(AKTØRID));
        Long fagsakId = fagsakRepository.opprettNy(fagsak);
        fagsakRepository.oppdaterRelasjonsRolle(fagsakId, RelasjonsRolleType.MORA);
        final Behandling.Builder builder = Behandling.forFørstegangssøknad(fagsak);
        final Behandling behandling = builder.build();
        Behandlingsresultat.opprettFor(behandling);
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
        final VilkårResultat nyttResultat = VilkårResultat.builder().buildFor(behandling);
        behandlingRepository.lagre(nyttResultat, behandlingRepository.taSkriveLås(behandling));
        return behandling;
    }

    private InntektArbeidYtelseAggregatBuilder opprettInntektArbeidYtelseAggregatForYrkesaktivitet(AktørId aktørId, InternArbeidsforholdRef ref,
            DatoIntervallEntitet periode, ArbeidType type,
            BigDecimal prosentsats, Arbeidsgiver virksomhet1) {
        InntektArbeidYtelseAggregatBuilder builder = InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.empty(), VersjonType.REGISTER);

        InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder aktørArbeidBuilder = builder.getAktørArbeidBuilder(aktørId);
        YrkesaktivitetBuilder yrkesaktivitetBuilder = aktørArbeidBuilder.getYrkesaktivitetBuilderForNøkkelAvType(
                new Opptjeningsnøkkel(ref, virksomhet1.getIdentifikator(), null), type);

        AktivitetsAvtaleBuilder aktivitetsAvtaleBuilder = yrkesaktivitetBuilder.getAktivitetsAvtaleBuilder();
        PermisjonBuilder permisjonBuilder = yrkesaktivitetBuilder.getPermisjonBuilder();

        AktivitetsAvtaleBuilder aktivitetsAvtale = aktivitetsAvtaleBuilder
                .medPeriode(periode)
                .medProsentsats(prosentsats)
                .medBeskrivelse("Ser greit ut");
        AktivitetsAvtaleBuilder ansettelsesperiode = yrkesaktivitetBuilder.getAktivitetsAvtaleBuilder()
                .medPeriode(periode);

        Permisjon permisjon = permisjonBuilder
                .medPermisjonsbeskrivelseType(PermisjonsbeskrivelseType.UTDANNINGSPERMISJON)
                .medPeriode(periode.getFomDato(), periode.getTomDato())
                .medProsentsats(BigDecimal.valueOf(100))
                .build();

        yrkesaktivitetBuilder
                .medArbeidType(type)
                .medArbeidsgiver(virksomhet1)
                .medArbeidsforholdId(ARBEIDSFORHOLD_ID)
                .leggTilPermisjon(permisjon)
                .leggTilAktivitetsAvtale(aktivitetsAvtale)
                .leggTilAktivitetsAvtale(ansettelsesperiode);

        InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder aktørArbeid = aktørArbeidBuilder
                .leggTilYrkesaktivitet(yrkesaktivitetBuilder);

        builder.leggTilAktørArbeid(aktørArbeid);

        return builder;
    }

    private void opprettInntektForFrilanser(InntektArbeidYtelseAggregatBuilder bekreftet, AktørId aktørId, InternArbeidsforholdRef ref,
            DatoIntervallEntitet periode,
            Arbeidsgiver virksomhet1) {
        InntektArbeidYtelseAggregatBuilder.AktørInntektBuilder ainntektBuilder = bekreftet.getAktørInntektBuilder(aktørId);
        InntektBuilder inntektBuilder = ainntektBuilder.getInntektBuilder(InntektsKilde.INNTEKT_OPPTJENING,
                new Opptjeningsnøkkel(ref, virksomhet1.getIdentifikator(), null));
        inntektBuilder.medArbeidsgiver(virksomhet1);
        inntektBuilder.leggTilInntektspost(InntektspostBuilder.ny().medInntektspostType(InntektspostType.LØNN)
                .medPeriode(periode.getFomDato(), periode.getTomDato()).medBeløp(BigDecimal.TEN));
        ainntektBuilder.leggTilInntekt(inntektBuilder);
        bekreftet.leggTilAktørInntekt(ainntektBuilder);
    }

}
