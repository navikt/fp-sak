package no.nav.foreldrepenger.web.app.tjenester.behandling.svp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingGrunnlagRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpAvklartOpphold;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpOppholdKilde;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpOppholdÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpTilretteleggingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpTilretteleggingFomKilde;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.TilretteleggingFOM;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.TilretteleggingType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerSvangerskapspenger;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.dbstoette.JpaExtension;
import no.nav.foreldrepenger.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.impl.ArbeidsforholdAdministrasjonTjeneste;
import no.nav.foreldrepenger.domene.arbeidsgiver.ArbeidsgiverOpplysninger;
import no.nav.foreldrepenger.domene.arbeidsgiver.ArbeidsgiverTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.AktivitetsAvtaleBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.foreldrepenger.domene.iay.modell.Permisjon;
import no.nav.foreldrepenger.domene.iay.modell.VersjonType;
import no.nav.foreldrepenger.domene.iay.modell.Yrkesaktivitet;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetBuilder;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.BekreftetPermisjonStatus;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.PermisjonsbeskrivelseType;
import no.nav.foreldrepenger.domene.registerinnhenting.StønadsperioderInnhenter;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.skjæringstidspunkt.OpplysningsPeriodeTjeneste;
import no.nav.vedtak.exception.FunksjonellException;

@ExtendWith(MockitoExtension.class)
@ExtendWith(JpaExtension.class)
class BekreftSvangerskapspengerOppdatererTest {

    private static final LocalDate BEHOV_FRA_DATO = LocalDate.now();
    private static final LocalDate TLR_FRA_2 = LocalDate.now().plusMonths(1);
    private static final LocalDate TERMINDATO = LocalDate.now().plusMonths(5);
    public static final String ARBEIDSGIVER_IDENT = "12378694712";
    public static final InternArbeidsforholdRef INTERN_ARBEIDSFORHOLD_REF = InternArbeidsforholdRef.nyRef();
    public static final InternArbeidsforholdRef INTERN_ARBEIDSFORHOLD_REF2 = InternArbeidsforholdRef.nyRef();
    private final InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();

    private BekreftSvangerskapspengerOppdaterer oppdaterer;
    private BehandlingRepositoryProvider repositoryProvider;
    private BehandlingGrunnlagRepositoryProvider grunnlagProvider;
    @Mock
    private StønadsperioderInnhenter stønadsperioderInnhenterMock;
    @Mock
    private ArbeidsgiverTjeneste arbeidsgiverTjeneste;

    @BeforeEach
    void beforeEach(EntityManager entityManager) {
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        grunnlagProvider = new BehandlingGrunnlagRepositoryProvider(entityManager);
        var arbeidsforholdAdministrasjonTjeneste = new ArbeidsforholdAdministrasjonTjeneste(
            inntektArbeidYtelseTjeneste);
        lenient().when(arbeidsgiverTjeneste.hent(any())).thenReturn(new ArbeidsgiverOpplysninger("123", "Arbeidsgiver"));
        var bekreftSvangerskapspengerHistorikkinnslagTjeneste = new BekreftSvangerskapspengerHistorikkinnslagTjeneste(
            arbeidsgiverTjeneste, repositoryProvider.getHistorikkinnslagRepository());
        oppdaterer = new BekreftSvangerskapspengerOppdaterer(grunnlagProvider, inntektArbeidYtelseTjeneste, stønadsperioderInnhenterMock, arbeidsforholdAdministrasjonTjeneste,
            repositoryProvider.getBehandlingRepository(), mock(OpplysningsPeriodeTjeneste.class), bekreftSvangerskapspengerHistorikkinnslagTjeneste);
    }

    @Test
    void skal_kunne_fjerne_permisjon_ved_flere_arbeidsforhold_i_samme_virksomhet_og_tilrettelegging_uten_id() {
        var behandling = behandlingMedTilretteleggingAP();

        var register = InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.empty(),
            VersjonType.REGISTER);

        var permisjonFom = LocalDate.of(2020, 2, 17);
        var permisjonTom = LocalDate.of(2020, 7, 12);
        var yrkesaktivitet = byggYrkesaktivitet(byggPermisjon(permisjonFom, permisjonTom), BEHOV_FRA_DATO,
            INTERN_ARBEIDSFORHOLD_REF);
        var yrkesaktivitet2 = byggYrkesaktivitet(null, TERMINDATO.plusMonths(10),
            INTERN_ARBEIDSFORHOLD_REF2);
        var aktørArbeidBuilder = InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder
            .oppdatere(Optional.empty())
            .medAktørId(behandling.getAktørId());
        aktørArbeidBuilder.leggTilYrkesaktivitet(yrkesaktivitet).leggTilYrkesaktivitet(yrkesaktivitet2);
        register.leggTilAktørArbeid(aktørArbeidBuilder);

        inntektArbeidYtelseTjeneste.lagreIayAggregat(behandling.getId(), register);
        var svpGrunnlag = byggSøknadsgrunnlag(behandling, false, false);

        var dto = byggDto(BEHOV_FRA_DATO, TERMINDATO,
            svpGrunnlag.getOpprinneligeTilrettelegginger().getTilretteleggingListe().get(0).getId(),
            true,
            new VelferdspermisjonDto(permisjonFom, permisjonTom, BigDecimal.valueOf(100), PermisjonsbeskrivelseType.VELFERDSPERMISJON, false),
            null,
            List.of(new SvpTilretteleggingDatoDto(BEHOV_FRA_DATO.plusWeeks(1), TilretteleggingType.INGEN_TILRETTELEGGING, null)),
            null);

        var param = new AksjonspunktOppdaterParameter(BehandlingReferanse.fra(behandling), dto);

        oppdaterer.oppdater(dto, param);

        var inntektArbeidYtelseGrunnlag = inntektArbeidYtelseTjeneste.hentGrunnlag(
            behandling.getId());

        var overstyringer = inntektArbeidYtelseGrunnlag.getArbeidsforholdOverstyringer();

        assertThat(overstyringer).hasSize(1);

        assertThat(overstyringer.get(0).getBekreftetPermisjon()).isPresent();
        assertThat(overstyringer.get(0).getBekreftetPermisjon().get().getStatus()).isEqualTo(BekreftetPermisjonStatus.IKKE_BRUK_PERMISJON);
        assertThat(overstyringer.get(0).getArbeidsgiver().getIdentifikator()).isEqualTo(ARBEIDSGIVER_IDENT);
        assertThat(overstyringer.get(0).getArbeidsforholdRef()).isEqualTo(INTERN_ARBEIDSFORHOLD_REF);
    }

    @Test
    void skal_sette_totrinn_ved_endring_tilretteleggingFoms() {
        var behandling = behandlingMedTilretteleggingAP();

        var register = InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.empty(),
            VersjonType.REGISTER);
        inntektArbeidYtelseTjeneste.lagreIayAggregat(behandling.getId(), register);

        var svpGrunnlag = byggSøknadsgrunnlag(behandling, true, false);
        //endrer
        var dto = byggDto(BEHOV_FRA_DATO, TERMINDATO,
            svpGrunnlag.getOpprinneligeTilrettelegginger().getTilretteleggingListe().get(0).getId(),
            true, null, null,
            List.of(new SvpTilretteleggingDatoDto(TLR_FRA_2, TilretteleggingType.INGEN_TILRETTELEGGING, null, null, SvpTilretteleggingFomKilde.REGISTRERT_AV_SAKSBEHANDLER,
                    null),
            new SvpTilretteleggingDatoDto(BEHOV_FRA_DATO, TilretteleggingType.DELVIS_TILRETTELEGGING, BigDecimal.valueOf(60), null, SvpTilretteleggingFomKilde.SØKNAD,
                null)),
            null);

        var param = new AksjonspunktOppdaterParameter(BehandlingReferanse.fra(behandling), dto);

        var resultat = oppdaterer.oppdater(dto, param);

        assertThat(resultat.kreverTotrinnsKontroll()).isTrue();
    }

    @Test
    void skal_sette_totrinn_ved_endring_skalBrukes() {
        var behandling = behandlingMedTilretteleggingAP();

        var register = InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.empty(),
            VersjonType.REGISTER);
        inntektArbeidYtelseTjeneste.lagreIayAggregat(behandling.getId(), register);

        var svpGrunnlag = byggSøknadsgrunnlag(behandling, true, false);
        var dto = byggDto(BEHOV_FRA_DATO, TERMINDATO,
            svpGrunnlag.getOpprinneligeTilrettelegginger().getTilretteleggingListe().get(0).getId(),
            false,
            null,
            INTERN_ARBEIDSFORHOLD_REF,
            List.of(new SvpTilretteleggingDatoDto(TLR_FRA_2, TilretteleggingType.INGEN_TILRETTELEGGING, null),
                new SvpTilretteleggingDatoDto(BEHOV_FRA_DATO, TilretteleggingType.DELVIS_TILRETTELEGGING, BigDecimal.valueOf(50))),
            null);
        var param = new AksjonspunktOppdaterParameter(BehandlingReferanse.fra(behandling), dto);

        var resultat = oppdaterer.oppdater(dto, param);

        assertThat(resultat.kreverTotrinnsKontroll()).isTrue();
    }

    @Test
    void skal_sette_totrinn_ved_endring_behovForTlrFom() {
        var behandling = behandlingMedTilretteleggingAP();

        var register = InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.empty(),
            VersjonType.REGISTER);
        inntektArbeidYtelseTjeneste.lagreIayAggregat(behandling.getId(), register);

        var svpGrunnlag = byggSøknadsgrunnlag(behandling, true, false);
        var dto = byggDto(BEHOV_FRA_DATO.minusDays(1), TERMINDATO,
            svpGrunnlag.getOpprinneligeTilrettelegginger().getTilretteleggingListe().get(0).getId(),
            true, null, null,
            List.of(new SvpTilretteleggingDatoDto(TLR_FRA_2, TilretteleggingType.INGEN_TILRETTELEGGING, null),
                new SvpTilretteleggingDatoDto(BEHOV_FRA_DATO, TilretteleggingType.DELVIS_TILRETTELEGGING, BigDecimal.valueOf(50))),
            null);
        var param = new AksjonspunktOppdaterParameter(BehandlingReferanse.fra(behandling), dto);

        var resultat = oppdaterer.oppdater(dto, param);

        assertThat(resultat.kreverTotrinnsKontroll()).isTrue();
    }

    @Test
    void skal_sette_totrinn_ved_endring_oppholdsPerioder() {
        var behandling = behandlingMedTilretteleggingAP();

        var register = InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.empty(),
            VersjonType.REGISTER);
        inntektArbeidYtelseTjeneste.lagreIayAggregat(behandling.getId(), register);

        var svpGrunnlag = byggSøknadsgrunnlag(behandling, true, false);
        //endrer
        var dto = byggDto(BEHOV_FRA_DATO, TERMINDATO,
            svpGrunnlag.getOpprinneligeTilrettelegginger().getTilretteleggingListe().get(0).getId(),
            true, null, null,
            List.of(new SvpTilretteleggingDatoDto(TLR_FRA_2, TilretteleggingType.INGEN_TILRETTELEGGING, null),
                new SvpTilretteleggingDatoDto(BEHOV_FRA_DATO, TilretteleggingType.DELVIS_TILRETTELEGGING, BigDecimal.valueOf(50), null, SvpTilretteleggingFomKilde.SØKNAD,
                    null)),
            List.of(new SvpAvklartOppholdPeriodeDto(BEHOV_FRA_DATO,BEHOV_FRA_DATO.plusWeeks(1), SvpOppholdÅrsak.SYKEPENGER, SvpAvklartOppholdPeriodeDto.SvpOppholdKilde.REGISTRERT_AV_SAKSBEHANDLER,false ),
                new SvpAvklartOppholdPeriodeDto(TLR_FRA_2, TLR_FRA_2.plusWeeks(3), SvpOppholdÅrsak.FERIE, SvpAvklartOppholdPeriodeDto.SvpOppholdKilde.REGISTRERT_AV_SAKSBEHANDLER, false)));

        var param = new AksjonspunktOppdaterParameter(BehandlingReferanse.fra(behandling), dto);

        var resultat = oppdaterer.oppdater(dto, param);

        assertThat(resultat.kreverTotrinnsKontroll()).isTrue();
    }
    @Test
    void ikke_totrinn_hvis_ingen_endring() {
        var behandling = behandlingMedTilretteleggingAP();

        var register = InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.empty(),
            VersjonType.REGISTER);
        inntektArbeidYtelseTjeneste.lagreIayAggregat(behandling.getId(), register);

        var svpGrunnlag = byggSøknadsgrunnlag(behandling, true, true);
        var dto = byggDto(BEHOV_FRA_DATO, LocalDate.now().plusDays(40),
            svpGrunnlag.getOpprinneligeTilrettelegginger().getTilretteleggingListe().get(0).getId(),
            true, null, null,
            List.of(new SvpTilretteleggingDatoDto(TLR_FRA_2, TilretteleggingType.INGEN_TILRETTELEGGING, null),
            new SvpTilretteleggingDatoDto(BEHOV_FRA_DATO, TilretteleggingType.DELVIS_TILRETTELEGGING, BigDecimal.valueOf(50))),
            List.of(new SvpAvklartOppholdPeriodeDto(BEHOV_FRA_DATO, BEHOV_FRA_DATO.plusWeeks(1), SvpOppholdÅrsak.SYKEPENGER, SvpAvklartOppholdPeriodeDto.SvpOppholdKilde.REGISTRERT_AV_SAKSBEHANDLER,false),
            new SvpAvklartOppholdPeriodeDto(TLR_FRA_2, TLR_FRA_2.plusWeeks(4), SvpOppholdÅrsak.FERIE, SvpAvklartOppholdPeriodeDto.SvpOppholdKilde.REGISTRERT_AV_SAKSBEHANDLER,false)));
        var param = new AksjonspunktOppdaterParameter(BehandlingReferanse.fra(behandling), dto);

        var resultat = oppdaterer.oppdater(dto, param);

        assertThat(resultat.kreverTotrinnsKontroll()).isFalse();
    }
    @Test
    void ikke_totrinn_hvis_ingen_endring_og_ingen_opphold() {
        var behandling = behandlingMedTilretteleggingAP();

        var register = InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.empty(),
            VersjonType.REGISTER);
        inntektArbeidYtelseTjeneste.lagreIayAggregat(behandling.getId(), register);

        var svpGrunnlag = byggSøknadsgrunnlag(behandling, true, false);
        var dto = byggDto(BEHOV_FRA_DATO, LocalDate.now().plusDays(40),
            svpGrunnlag.getOpprinneligeTilrettelegginger().getTilretteleggingListe().get(0).getId(),
            true, null, null,
            List.of(new SvpTilretteleggingDatoDto(TLR_FRA_2, TilretteleggingType.INGEN_TILRETTELEGGING, null),
                new SvpTilretteleggingDatoDto(BEHOV_FRA_DATO, TilretteleggingType.DELVIS_TILRETTELEGGING, BigDecimal.valueOf(50))),
            Collections.emptyList());
        var param = new AksjonspunktOppdaterParameter(BehandlingReferanse.fra(behandling), dto);

        var resultat = oppdaterer.oppdater(dto, param);

        assertThat(resultat.kreverTotrinnsKontroll()).isFalse();
    }

    @Test
    void skal_feile_ved_like_tilretteleggingsdatoer() {
        var dto = byggDto(BEHOV_FRA_DATO, TERMINDATO, 123L,
            true, null, null,
            List.of(new SvpTilretteleggingDatoDto(BEHOV_FRA_DATO.plusWeeks(1), TilretteleggingType.INGEN_TILRETTELEGGING, null),
            new SvpTilretteleggingDatoDto(BEHOV_FRA_DATO.plusWeeks(1), TilretteleggingType.HEL_TILRETTELEGGING, null)),
            null);

        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.VURDER_SVP_TILRETTELEGGING,
            BehandlingStegType.VURDER_TILRETTELEGGING);
        var behandling = scenario.lagre(repositoryProvider);
        var param = new AksjonspunktOppdaterParameter(BehandlingReferanse.fra(behandling), dto);

        assertThatThrownBy(() -> oppdaterer.oppdater(dto, param)).isInstanceOf(FunksjonellException.class)
            .hasMessageContaining("FP-682318");
    }

    @Test
    void stillingsprosent_skal_kunne_være_null_når_arbeidsforholdet_ikke_skal_brukes() {
        var behandling = behandlingMedTilretteleggingAP();
        var register = InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.empty(),
            VersjonType.REGISTER);
        inntektArbeidYtelseTjeneste.lagreIayAggregat(behandling.getId(), register);

        var svpGrunnlag = byggSøknadsgrunnlag(behandling, false, false);
        var dto = byggDto(BEHOV_FRA_DATO, TERMINDATO,
            svpGrunnlag.getOpprinneligeTilrettelegginger().getTilretteleggingListe().get(0).getId(), false,
            null, INTERN_ARBEIDSFORHOLD_REF,
            List.of(new SvpTilretteleggingDatoDto(BEHOV_FRA_DATO.plusWeeks(1), TilretteleggingType.DELVIS_TILRETTELEGGING, null)),
            null);

        var param = new AksjonspunktOppdaterParameter(BehandlingReferanse.fra(behandling), dto);
        assertDoesNotThrow(() -> oppdaterer.oppdater(dto, param));
    }

    private Behandling behandlingMedTilretteleggingAP() {
        var scenario = ScenarioMorSøkerSvangerskapspenger.forSvangerskapspenger();
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.VURDER_SVP_TILRETTELEGGING,
            BehandlingStegType.VURDER_TILRETTELEGGING);
        scenario.medDefaultBekreftetTerminbekreftelse();
        return scenario.lagre(repositoryProvider);
    }

    private SvpGrunnlagEntitet byggSøknadsgrunnlag(Behandling behandling, boolean flereFoms, boolean medAvklartOpphold) {
        SvpTilretteleggingEntitet tilrettelegging;

        if (flereFoms) {
            var tilr2Fom1 = opprettTilrFom(BEHOV_FRA_DATO, 50, TilretteleggingType.DELVIS_TILRETTELEGGING, LocalDate.now().minusDays(1));
            var tilr2Fom2 = opprettTilrFom(TLR_FRA_2, 0, TilretteleggingType.INGEN_TILRETTELEGGING, LocalDate.now());

            var tilrBuilder = new SvpTilretteleggingEntitet.Builder().medBehovForTilretteleggingFom(BEHOV_FRA_DATO)
                .medTilretteleggingFraDatoer(List.of(tilr2Fom1, tilr2Fom2))
                .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
                .medArbeidsgiver(Arbeidsgiver.virksomhet("123456789"))
                .medMottattTidspunkt(LocalDateTime.now())
                .medKopiertFraTidligereBehandling(false);
            if (medAvklartOpphold){
                var tilr2Opphold1 = opprettOpphold(BEHOV_FRA_DATO, BEHOV_FRA_DATO.plusWeeks(1), SvpOppholdÅrsak.SYKEPENGER);
                var tilr2Opphold2 = opprettOpphold(TLR_FRA_2, TLR_FRA_2.plusWeeks(4), SvpOppholdÅrsak.FERIE);
                tilrBuilder.medAvklarteOpphold(List.of(tilr2Opphold1, tilr2Opphold2));
            }
            tilrettelegging = tilrBuilder.build();
        } else {
            var tilr1Fom = opprettTilrFom(BEHOV_FRA_DATO, 0, TilretteleggingType.INGEN_TILRETTELEGGING, LocalDate.now().minusDays(1));

           var tilrBuilder  = new SvpTilretteleggingEntitet.Builder().medBehovForTilretteleggingFom(BEHOV_FRA_DATO)
                .medTilretteleggingFraDatoer(List.of(tilr1Fom))
                .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
                .medArbeidsgiver(Arbeidsgiver.virksomhet("987654321"))
                .medMottattTidspunkt(LocalDateTime.now())
                .medKopiertFraTidligereBehandling(false);

            if (medAvklartOpphold) {
                var tilr1Opphold = opprettOpphold(BEHOV_FRA_DATO, BEHOV_FRA_DATO.plusWeeks(1), SvpOppholdÅrsak.SYKEPENGER);
                tilrBuilder.medAvklartOpphold(tilr1Opphold);

            }
            tilrettelegging = tilrBuilder.build();
        }

        var svpGrunnlag = new SvpGrunnlagEntitet.Builder().medBehandlingId(behandling.getId())
            .medOpprinneligeTilrettelegginger(List.of(tilrettelegging))
            .build();
        grunnlagProvider.getSvangerskapspengerRepository().lagreOgFlush(svpGrunnlag);
        return svpGrunnlag;
    }

    private SvpAvklartOpphold opprettOpphold(LocalDate fom, LocalDate tom, SvpOppholdÅrsak årsak) {
        return SvpAvklartOpphold.Builder.nytt()
                .medOppholdÅrsak(årsak)
                .medOppholdPeriode(fom, tom)
                .medKilde(SvpOppholdKilde.REGISTRERT_AV_SAKSBEHANDLER)
                .build();
    }

    private TilretteleggingFOM opprettTilrFom(LocalDate fraDato, long stilingsprosent, TilretteleggingType type, LocalDate tidligsMottattDato) {
        var tilretteleggingFomBuilder = new TilretteleggingFOM.Builder()
            .medFomDato(fraDato)
            .medTilretteleggingType(type)
            .medTidligstMottattDato(tidligsMottattDato);

        if (stilingsprosent > 0) {
            tilretteleggingFomBuilder.medStillingsprosent(BigDecimal.valueOf(stilingsprosent));
        }

            return tilretteleggingFomBuilder.build();
    }


    private BekreftSvangerskapspengerDto byggDto(LocalDate behovDato,
                                                 LocalDate termindato,
                                                 Long id,
                                                 boolean skalBrukes,
                                                 VelferdspermisjonDto permisjonDto,
                                                 InternArbeidsforholdRef internArbeidsforholdRef,
                                                 List<SvpTilretteleggingDatoDto> tilretteleggingDatoer,
                                                 List<SvpAvklartOppholdPeriodeDto> avklarteOppholdPerioder) {
        var dto = new BekreftSvangerskapspengerDto("Velbegrunnet begrunnelse");
        dto.setTermindato(termindato);

        var arbeidsforholdDto = new SvpArbeidsforholdDto();
        arbeidsforholdDto.setTilretteleggingBehovFom(behovDato);

        arbeidsforholdDto.setTilretteleggingDatoer(tilretteleggingDatoer);
        arbeidsforholdDto.setAvklarteOppholdPerioder(avklarteOppholdPerioder);
        arbeidsforholdDto.setArbeidsgiverReferanse(ARBEIDSGIVER_IDENT);
        arbeidsforholdDto.setInternArbeidsforholdReferanse(
            internArbeidsforholdRef == null ? null : internArbeidsforholdRef.getReferanse());
        arbeidsforholdDto.setTilretteleggingId(id);
        arbeidsforholdDto.setSkalBrukes(skalBrukes);
        if (permisjonDto != null) {
            arbeidsforholdDto.setVelferdspermisjoner(List.of(permisjonDto));
        }

        dto.setBekreftetSvpArbeidsforholdList(List.of(arbeidsforholdDto));
        return dto;
    }

    private Permisjon byggPermisjon(LocalDate permisjonFom, LocalDate permisjonTom) {
        return YrkesaktivitetBuilder.nyPermisjonBuilder()
            .medProsentsats(BigDecimal.valueOf(100))
            .medPermisjonsbeskrivelseType(PermisjonsbeskrivelseType.VELFERDSPERMISJON)
            .medPeriode(permisjonFom, permisjonTom)
            .build();
    }

    private Yrkesaktivitet byggYrkesaktivitet(Permisjon permisjon,
                                              LocalDate fomDato,
                                              InternArbeidsforholdRef internArbeidsforholdRef) {
        var yrkesaktivitetBuilder = YrkesaktivitetBuilder.oppdatere(Optional.empty())
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .medArbeidsgiver(Arbeidsgiver.virksomhet(ARBEIDSGIVER_IDENT))
            .medArbeidsforholdId(internArbeidsforholdRef)
            .leggTilAktivitetsAvtale(AktivitetsAvtaleBuilder.ny()
                .medPeriode(DatoIntervallEntitet.fraOgMed(fomDato))
                .medProsentsats(BigDecimal.ZERO))
            .leggTilAktivitetsAvtale(AktivitetsAvtaleBuilder.ny().medPeriode(DatoIntervallEntitet.fraOgMed(fomDato)));
        if (permisjon != null) {
            yrkesaktivitetBuilder.leggTilPermisjon(permisjon);
        }
        return yrkesaktivitetBuilder.build();
    }
}
