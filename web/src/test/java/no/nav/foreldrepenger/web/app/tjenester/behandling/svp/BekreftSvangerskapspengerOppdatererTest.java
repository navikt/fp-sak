package no.nav.foreldrepenger.web.app.tjenester.behandling.svp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import javax.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpTilretteleggingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.TilretteleggingFOM;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.TilretteleggingFilter;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.TilretteleggingType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.dbstoette.FPsakEntityManagerAwareExtension;
import no.nav.foreldrepenger.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.AktivitetsAvtaleBuilder;
import no.nav.foreldrepenger.domene.iay.modell.AktørArbeid;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregat;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.Permisjon;
import no.nav.foreldrepenger.domene.iay.modell.PermisjonBuilder;
import no.nav.foreldrepenger.domene.iay.modell.VersjonType;
import no.nav.foreldrepenger.domene.iay.modell.Yrkesaktivitet;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetBuilder;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.PermisjonsbeskrivelseType;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;
import no.nav.foreldrepenger.tilganger.InnloggetNavAnsattDto;
import no.nav.foreldrepenger.tilganger.TilgangerTjeneste;
import no.nav.vedtak.exception.FunksjonellException;

@ExtendWith(MockitoExtension.class)
@ExtendWith(FPsakEntityManagerAwareExtension.class)
public class BekreftSvangerskapspengerOppdatererTest {

    private static final LocalDate BEHOV_DATO = LocalDate.now();
    private static final LocalDate TERMINDATO = LocalDate.now().plusMonths(5);
    public static final String ARBEIDSGIVER_IDENT = "12378694712";
    public static final InternArbeidsforholdRef INTERN_ARBEIDSFORHOLD_REF = InternArbeidsforholdRef.nyRef();
    public static final InternArbeidsforholdRef INTERN_ARBEIDSFORHOLD_REF2 = InternArbeidsforholdRef.nyRef();

    @Mock
    private TilgangerTjeneste tilgangerTjenesteMock;
    private final InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();

    private BekreftSvangerskapspengerOppdaterer oppdaterer;
    private BehandlingRepositoryProvider repositoryProvider;

    @BeforeEach
    public void beforeEach(EntityManager entityManager) {
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        HistorikkTjenesteAdapter historikkAdapter = new HistorikkTjenesteAdapter(
            repositoryProvider.getHistorikkRepository(), null);
        oppdaterer = new BekreftSvangerskapspengerOppdaterer(historikkAdapter, repositoryProvider,
            tilgangerTjenesteMock, inntektArbeidYtelseTjeneste);
    }

    @Test
    public void skal_kunne_fjerne_permisjon_ved_flere_arbeidsforhold_i_samme_virksomhet_og_tilrettelegging_uten_id() {
        Behandling behandling = behandlingMedTilretteleggingAP();

        InntektArbeidYtelseAggregatBuilder register = InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.empty(),
            VersjonType.REGISTER);

        LocalDate permisjonFom = LocalDate.of(2020, 2, 17);
        LocalDate permisjonTom = LocalDate.of(2020, 7, 12);
        Yrkesaktivitet yrkesaktivitet = byggYrkesaktivitet(byggPermisjon(permisjonFom, permisjonTom), BEHOV_DATO,
            INTERN_ARBEIDSFORHOLD_REF);
        Yrkesaktivitet yrkesaktivitet2 = byggYrkesaktivitet(null, TERMINDATO.plusMonths(10),
            INTERN_ARBEIDSFORHOLD_REF2);
        InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder aktørArbeidBuilder = InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder
            .oppdatere(Optional.empty())
            .medAktørId(behandling.getAktørId());
        aktørArbeidBuilder.leggTilYrkesaktivitet(yrkesaktivitet).leggTilYrkesaktivitet(yrkesaktivitet2);
        register.leggTilAktørArbeid(aktørArbeidBuilder);

        inntektArbeidYtelseTjeneste.lagreIayAggregat(behandling.getId(), register);
        SvpGrunnlagEntitet svpGrunnlag = byggSøknadsgrunnlag(behandling);

        BekreftSvangerskapspengerDto dto = byggDto(BEHOV_DATO, TERMINDATO,
            svpGrunnlag.getOpprinneligeTilrettelegginger().getTilretteleggingListe().get(0).getId(),
            new VelferdspermisjonDto(permisjonFom, permisjonTom, BigDecimal.valueOf(100),
                PermisjonsbeskrivelseType.VELFERDSPERMISJON, false), null,
            new SvpTilretteleggingDatoDto(BEHOV_DATO.plusWeeks(1), TilretteleggingType.INGEN_TILRETTELEGGING, null));

        AksjonspunktOppdaterParameter param = new AksjonspunktOppdaterParameter(behandling, Optional.empty(), dto);

        OppdateringResultat resultat = oppdaterer.oppdater(dto, param);

        InntektArbeidYtelseGrunnlag inntektArbeidYtelseGrunnlag = inntektArbeidYtelseTjeneste.hentGrunnlag(
            behandling.getId());
        boolean permisjonErFjernet = inntektArbeidYtelseGrunnlag.getSaksbehandletVersjon()
            .orElseThrow()
            .getAktørArbeid()
            .iterator()
            .next()
            .hentAlleYrkesaktiviteter()
            .stream()
            .allMatch(ya -> ya.getPermisjon().isEmpty());

        assertThat(permisjonErFjernet).isTrue();

        assertThat(resultat.kreverTotrinnsKontroll()).isTrue();
    }


    @Test
    public void skal_sette_totrinn_ved_endring() {
        Behandling behandling = behandlingMedTilretteleggingAP();

        InntektArbeidYtelseAggregatBuilder register = InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.empty(),
            VersjonType.REGISTER);
        inntektArbeidYtelseTjeneste.lagreIayAggregat(behandling.getId(), register);

        SvpGrunnlagEntitet svpGrunnlag = byggSøknadsgrunnlag(behandling);
        BekreftSvangerskapspengerDto dto = byggDto(BEHOV_DATO, TERMINDATO,
            svpGrunnlag.getOpprinneligeTilrettelegginger().getTilretteleggingListe().get(0).getId(),
            new SvpTilretteleggingDatoDto(BEHOV_DATO.plusWeeks(1), TilretteleggingType.INGEN_TILRETTELEGGING, null));
        AksjonspunktOppdaterParameter param = new AksjonspunktOppdaterParameter(behandling, Optional.empty(), dto);

        OppdateringResultat resultat = oppdaterer.oppdater(dto, param);

        assertThat(resultat.kreverTotrinnsKontroll()).isTrue();
    }

    @Test
    public void skal_feile_ved_like_tilretteleggingsdatoer() {
        BekreftSvangerskapspengerDto dto = byggDto(BEHOV_DATO, TERMINDATO, 123L,
            new SvpTilretteleggingDatoDto(BEHOV_DATO.plusWeeks(1), TilretteleggingType.INGEN_TILRETTELEGGING, null),
            new SvpTilretteleggingDatoDto(BEHOV_DATO.plusWeeks(1), TilretteleggingType.HEL_TILRETTELEGGING, null));

        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.VURDER_SVP_TILRETTELEGGING,
            BehandlingStegType.VURDER_TILRETTELEGGING);
        Behandling behandling = scenario.lagre(repositoryProvider);
        AksjonspunktOppdaterParameter param = new AksjonspunktOppdaterParameter(behandling, Optional.empty(), dto);

        assertThatThrownBy(() -> oppdaterer.oppdater(dto, param)).isInstanceOf(FunksjonellException.class)
            .hasMessageContaining("FP-682318");
    }

    @Test
    public void skal_kunne_overstyre_utbetalinsgrad_dersom_ansatt_har_rolle_overstyrer() {
        settOppTilgangTilOverstyring(true);
        Behandling behandling = behandlingMedTilretteleggingAP();

        InntektArbeidYtelseAggregatBuilder register = InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.empty(),
            VersjonType.REGISTER);
        inntektArbeidYtelseTjeneste.lagreIayAggregat(behandling.getId(), register);

        SvpGrunnlagEntitet svpGrunnlag = byggSøknadsgrunnlag(behandling);
        BekreftSvangerskapspengerDto dto = byggDto(BEHOV_DATO, TERMINDATO,
            svpGrunnlag.getOpprinneligeTilrettelegginger().getTilretteleggingListe().get(0).getId(),
            new SvpTilretteleggingDatoDto(BEHOV_DATO.plusWeeks(1), TilretteleggingType.DELVIS_TILRETTELEGGING,
                new BigDecimal("30.00"), new BigDecimal("40.00")));
        AksjonspunktOppdaterParameter param = new AksjonspunktOppdaterParameter(behandling, Optional.empty(), dto);

        OppdateringResultat resultat = oppdaterer.oppdater(dto, param);

        assertThat(resultat.kreverTotrinnsKontroll()).isTrue();
        var oppdatertGrunnlag = repositoryProvider.getSvangerskapspengerRepository().hentGrunnlag(behandling.getId());
        var tilrettelegginger = new TilretteleggingFilter(
            oppdatertGrunnlag.orElseThrow()).getAktuelleTilretteleggingerUfiltrert();
        assertThat(tilrettelegginger).hasSize(1);
        var endretTilrettelegging = tilrettelegginger.get(0);
        assertThat(endretTilrettelegging.getTilretteleggingFOMListe()).hasSize(1);
        var endretTilretteleggingDato = endretTilrettelegging.getTilretteleggingFOMListe().get(0);
        assertThat(endretTilretteleggingDato.getFomDato()).isEqualTo(BEHOV_DATO.plusWeeks(1));
        assertThat(endretTilretteleggingDato.getType()).isEqualTo(TilretteleggingType.DELVIS_TILRETTELEGGING);
        assertThat(endretTilretteleggingDato.getStillingsprosent()).isEqualByComparingTo(new BigDecimal("30.00"));
        assertThat(endretTilretteleggingDato.getOverstyrtUtbetalingsgrad()).isEqualByComparingTo(
            new BigDecimal("40.00"));
    }

    @Test
    public void skal_ikke_kunne_overstyre_utbetalinsgrad_dersom_ansatt_ikke_har_rolle_overstyrer() {
        settOppTilgangTilOverstyring(false);
        Behandling behandling = behandlingMedTilretteleggingAP();

        SvpGrunnlagEntitet svpGrunnlag = byggSøknadsgrunnlag(behandling);
        BekreftSvangerskapspengerDto dto = byggDto(BEHOV_DATO, TERMINDATO,
            svpGrunnlag.getOpprinneligeTilrettelegginger().getTilretteleggingListe().get(0).getId(),
            new SvpTilretteleggingDatoDto(BEHOV_DATO.plusWeeks(1), TilretteleggingType.DELVIS_TILRETTELEGGING,
                new BigDecimal("20.00"), new BigDecimal("40.00")));
        AksjonspunktOppdaterParameter param = new AksjonspunktOppdaterParameter(behandling, Optional.empty(), dto);

        assertThatThrownBy(() -> oppdaterer.oppdater(dto, param)).isInstanceOf(FunksjonellException.class)
            .hasMessageContaining("FP-682319");
    }

    @Test
    public void stillingsprosent_skal_kunne_være_null_når_arbeidsforholdet_ikke_skal_brukes() {
        var behandling = behandlingMedTilretteleggingAP();
        InntektArbeidYtelseAggregatBuilder register = InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.empty(),
            VersjonType.REGISTER);
        inntektArbeidYtelseTjeneste.lagreIayAggregat(behandling.getId(), register);

        var svpGrunnlag = byggSøknadsgrunnlag(behandling);
        var dto = byggDto(BEHOV_DATO, TERMINDATO,
            svpGrunnlag.getOpprinneligeTilrettelegginger().getTilretteleggingListe().get(0).getId(), false,
            new SvpTilretteleggingDatoDto(BEHOV_DATO.plusWeeks(1), TilretteleggingType.DELVIS_TILRETTELEGGING, null));

        var param = new AksjonspunktOppdaterParameter(behandling, Optional.empty(), dto);
        assertDoesNotThrow(() -> oppdaterer.oppdater(dto, param));
    }

    @Test
    public void skal_fjerne_permisjon_ved_ugyldig_velferdspermisjon() {
        Behandling behandling = behandlingMedTilretteleggingAP();

        InntektArbeidYtelseAggregatBuilder register = InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.empty(),
            VersjonType.REGISTER);
        InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder aktørArbeidBuilder = register.getAktørArbeidBuilder(
            behandling.getAktørId());
        YrkesaktivitetBuilder yrkesaktivitetBuilder = YrkesaktivitetBuilder.oppdatere(Optional.empty());
        yrkesaktivitetBuilder.medArbeidsgiver(Arbeidsgiver.virksomhet(ARBEIDSGIVER_IDENT));
        yrkesaktivitetBuilder.medArbeidsforholdId(INTERN_ARBEIDSFORHOLD_REF);
        yrkesaktivitetBuilder.medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);
        PermisjonBuilder permisjonBuilder = yrkesaktivitetBuilder.getPermisjonBuilder();
        BigDecimal permisjonsprosent = BigDecimal.valueOf(40);
        permisjonBuilder.medProsentsats(permisjonsprosent);
        permisjonBuilder.medPermisjonsbeskrivelseType(PermisjonsbeskrivelseType.VELFERDSPERMISJON);
        permisjonBuilder.medPeriode(BEHOV_DATO, TERMINDATO);
        aktørArbeidBuilder.leggTilYrkesaktivitet(yrkesaktivitetBuilder);
        register.leggTilAktørArbeid(aktørArbeidBuilder);
        inntektArbeidYtelseTjeneste.lagreIayAggregat(behandling.getId(), register);

        SvpGrunnlagEntitet svpGrunnlag = byggSøknadsgrunnlag(behandling);
        BekreftSvangerskapspengerDto dto = byggDto(BEHOV_DATO, TERMINDATO,
            svpGrunnlag.getOpprinneligeTilrettelegginger().getTilretteleggingListe().get(0).getId(),
            new SvpTilretteleggingDatoDto(BEHOV_DATO.plusWeeks(1), TilretteleggingType.INGEN_TILRETTELEGGING, null));
        AksjonspunktOppdaterParameter param = new AksjonspunktOppdaterParameter(behandling, Optional.empty(), dto);
        SvpArbeidsforholdDto svpArbeidsforholdDto = dto.getBekreftetSvpArbeidsforholdList().get(0);
        svpArbeidsforholdDto.setVelferdspermisjoner(List.of(
            new VelferdspermisjonDto(BEHOV_DATO, TERMINDATO, permisjonsprosent,
                PermisjonsbeskrivelseType.VELFERDSPERMISJON, false)));
        OppdateringResultat resultat = oppdaterer.oppdater(dto, param);

        Optional<InntektArbeidYtelseAggregat> saksbehandletVersjon = inntektArbeidYtelseTjeneste.hentGrunnlag(
            behandling.getId()).getSaksbehandletVersjon();

        assertThat(saksbehandletVersjon).isPresent();
        AktørArbeid aktørArbeid = saksbehandletVersjon.get().getAktørArbeid().iterator().next();
        Collection<Permisjon> permisjoner = aktørArbeid.hentAlleYrkesaktiviteter().iterator().next().getPermisjon();
        assertThat(permisjoner.isEmpty()).isTrue();
    }

    private Behandling behandlingMedTilretteleggingAP() {
        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.VURDER_SVP_TILRETTELEGGING,
            BehandlingStegType.VURDER_TILRETTELEGGING);
        scenario.medDefaultBekreftetTerminbekreftelse();
        return scenario.lagre(repositoryProvider);
    }

    private void settOppTilgangTilOverstyring(boolean kanOverstyre) {
        var dto = new InnloggetNavAnsattDto.Builder().setBrukernavn("mrOverstyrer")
            .setNavn("Mr Overstyrer")
            .setKanBehandleKode6(false)
            .setKanBehandleKode7(false)
            .setKanBehandleKodeEgenAnsatt(false)
            .setKanOverstyre(kanOverstyre)
            .setKanBeslutte(true)
            .setKanVeilede(true)
            .setKanSaksbehandle(true)
            .skalViseDetaljerteFeilmeldinger(true)
            .create();
        when(tilgangerTjenesteMock.innloggetBruker()).thenReturn(dto);
    }

    private SvpGrunnlagEntitet byggSøknadsgrunnlag(Behandling behandling) {
        SvpTilretteleggingEntitet tilrettelegging = new SvpTilretteleggingEntitet.Builder().medBehovForTilretteleggingFom(
            BEHOV_DATO)
            .medIngenTilrettelegging(BEHOV_DATO)
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .medArbeidsgiver(Arbeidsgiver.person(AktørId.dummy()))
            .medMottattTidspunkt(LocalDateTime.now())
            .medKopiertFraTidligereBehandling(false)
            .medTilretteleggingFom(new TilretteleggingFOM.Builder().medFomDato(BEHOV_DATO)
                .medTilretteleggingType(TilretteleggingType.INGEN_TILRETTELEGGING)
                .build())
            .build();
        SvpGrunnlagEntitet svpGrunnlag = new SvpGrunnlagEntitet.Builder().medBehandlingId(behandling.getId())
            .medOpprinneligeTilrettelegginger(List.of(tilrettelegging))
            .build();
        repositoryProvider.getSvangerskapspengerRepository().lagreOgFlush(svpGrunnlag);
        return svpGrunnlag;
    }


    private BekreftSvangerskapspengerDto byggDto(LocalDate behovDato,
                                                 LocalDate termindato,
                                                 Long id,
                                                 VelferdspermisjonDto permisjonDto,
                                                 InternArbeidsforholdRef internArbeidsforholdRef,
                                                 SvpTilretteleggingDatoDto... tilretteleggingDatoer) {
        return byggDto(behovDato, termindato, id, true, permisjonDto, internArbeidsforholdRef, tilretteleggingDatoer);
    }


    private BekreftSvangerskapspengerDto byggDto(LocalDate behovDato,
                                                 LocalDate termindato,
                                                 Long id,
                                                 SvpTilretteleggingDatoDto... tilretteleggingDatoer) {
        return byggDto(behovDato, termindato, id, true, tilretteleggingDatoer);
    }

    private BekreftSvangerskapspengerDto byggDto(LocalDate behovDato,
                                                 LocalDate termindato,
                                                 Long id,
                                                 boolean skalBrukes,
                                                 SvpTilretteleggingDatoDto... tilretteleggingDatoer) {
        return byggDto(behovDato, termindato, id, skalBrukes, null, INTERN_ARBEIDSFORHOLD_REF, tilretteleggingDatoer);
    }

    private BekreftSvangerskapspengerDto byggDto(LocalDate behovDato,
                                                 LocalDate termindato,
                                                 Long id,
                                                 boolean skalBrukes,
                                                 VelferdspermisjonDto permisjonDto,
                                                 InternArbeidsforholdRef internArbeidsforholdRef,
                                                 SvpTilretteleggingDatoDto... tilretteleggingDatoer) {
        BekreftSvangerskapspengerDto dto = new BekreftSvangerskapspengerDto("Velbegrunnet begrunnelse");
        dto.setTermindato(termindato);

        SvpArbeidsforholdDto arbeidsforholdDto = new SvpArbeidsforholdDto();
        arbeidsforholdDto.setTilretteleggingBehovFom(behovDato);
        List<SvpTilretteleggingDatoDto> datoer = new ArrayList<>(Arrays.asList(tilretteleggingDatoer));

        arbeidsforholdDto.setTilretteleggingDatoer(datoer);
        arbeidsforholdDto.setArbeidsgiverReferanse(ARBEIDSGIVER_IDENT);
        arbeidsforholdDto.setInternArbeidsforholdReferanse(
            internArbeidsforholdRef == null ? null : internArbeidsforholdRef.getReferanse());
        arbeidsforholdDto.setMottattTidspunkt(LocalDateTime.now());
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
        YrkesaktivitetBuilder yrkesaktivitetBuilder = YrkesaktivitetBuilder.oppdatere(Optional.empty())
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
