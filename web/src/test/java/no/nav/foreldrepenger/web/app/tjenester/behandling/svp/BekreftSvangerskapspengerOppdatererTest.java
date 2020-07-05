package no.nav.foreldrepenger.web.app.tjenester.behandling.svp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvangerskapspengerRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpTilretteleggingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.TilretteleggingFOM;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.TilretteleggingFilter;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.TilretteleggingType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;
import no.nav.foreldrepenger.historikk.dto.HistorikkInnslagKonverter;
import no.nav.foreldrepenger.tilganger.InnloggetNavAnsattDto;
import no.nav.foreldrepenger.tilganger.TilgangerTjeneste;
import no.nav.vedtak.exception.FunksjonellException;

public class BekreftSvangerskapspengerOppdatererTest {

    private static final LocalDate BEHOV_DATO = LocalDate.now();
    private static final LocalDate TERMINDATO = LocalDate.now().plusMonths(5);

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Rule
    public UnittestRepositoryRule repoRule = new UnittestRepositoryRule();
    private BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(repoRule.getEntityManager());
    private HistorikkTjenesteAdapter historikkAdapter = new HistorikkTjenesteAdapter(repositoryProvider.getHistorikkRepository(),
        new HistorikkInnslagKonverter(),
        null);
    private final SvangerskapspengerRepository svangerskapspengerRepository = repositoryProvider.getSvangerskapspengerRepository();
    private FamilieHendelseRepository familieHendelseRepository = repositoryProvider.getFamilieHendelseRepository();
    private TilgangerTjeneste tilgangerTjenesteMock = mock(TilgangerTjeneste.class);

    private BekreftSvangerskapspengerOppdaterer oppdaterer = new BekreftSvangerskapspengerOppdaterer(svangerskapspengerRepository, historikkAdapter, repositoryProvider, familieHendelseRepository, tilgangerTjenesteMock);

    @Test
    public void skal_sette_totrinn_ved_endring() {
        Behandling behandling = behandlingMedTilretteleggingAP();

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
            new SvpTilretteleggingDatoDto(BEHOV_DATO.plusWeeks(1), TilretteleggingType.HEL_TILRETTELEGGING, null)
        );

        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.VURDER_SVP_TILRETTELEGGING, BehandlingStegType.VURDER_TILRETTELEGGING);
        Behandling behandling = scenario.lagre(repositoryProvider);
        AksjonspunktOppdaterParameter param = new AksjonspunktOppdaterParameter(behandling, Optional.empty(), dto);

        expectedException.expect(FunksjonellException.class);
        expectedException.expectMessage("FP-682318");

        oppdaterer.oppdater(dto, param);
    }

    @Test
    public void skal_kunne_overstyre_utbetalinsgrad_dersom_ansatt_har_rolle_overstyrer() {
        settOppTilgangTilOverstyring(true);
        Behandling behandling = behandlingMedTilretteleggingAP();

        SvpGrunnlagEntitet svpGrunnlag = byggSøknadsgrunnlag(behandling);
        BekreftSvangerskapspengerDto dto = byggDto(BEHOV_DATO, TERMINDATO,
            svpGrunnlag.getOpprinneligeTilrettelegginger().getTilretteleggingListe().get(0).getId(),
            new SvpTilretteleggingDatoDto(BEHOV_DATO.plusWeeks(1), TilretteleggingType.DELVIS_TILRETTELEGGING, new BigDecimal("30.00"), new BigDecimal("40.00")));
        AksjonspunktOppdaterParameter param = new AksjonspunktOppdaterParameter(behandling, Optional.empty(), dto);

        OppdateringResultat resultat = oppdaterer.oppdater(dto, param);

        assertThat(resultat.kreverTotrinnsKontroll()).isTrue();
        var oppdatertGrunnlag = svangerskapspengerRepository.hentGrunnlag(behandling.getId());
        var tilrettelegginger = new TilretteleggingFilter(oppdatertGrunnlag.get()).getAktuelleTilretteleggingerUfiltrert();
        assertThat(tilrettelegginger).hasSize(1);
        var endretTilrettelegging = tilrettelegginger.get(0);
        assertThat(endretTilrettelegging.getTilretteleggingFOMListe()).hasSize(1);
        var endretTilretteleggingDato = endretTilrettelegging.getTilretteleggingFOMListe().get(0);
        assertThat(endretTilretteleggingDato.getFomDato()).isEqualTo(BEHOV_DATO.plusWeeks(1));
        assertThat(endretTilretteleggingDato.getType()).isEqualTo(TilretteleggingType.DELVIS_TILRETTELEGGING);
        assertThat(endretTilretteleggingDato.getStillingsprosent()).isEqualByComparingTo(new BigDecimal("30.00"));
        assertThat(endretTilretteleggingDato.getOverstyrtUtbetalingsgrad()).isEqualByComparingTo(new BigDecimal("40.00"));
    }

    @Test
    public void skal_ikke_kunne_overstyre_utbetalinsgrad_dersom_ansatt_ikke_har_rolle_overstyrer() {
        settOppTilgangTilOverstyring(false);
        Behandling behandling = behandlingMedTilretteleggingAP();

        SvpGrunnlagEntitet svpGrunnlag = byggSøknadsgrunnlag(behandling);
        BekreftSvangerskapspengerDto dto = byggDto(BEHOV_DATO, TERMINDATO,
            svpGrunnlag.getOpprinneligeTilrettelegginger().getTilretteleggingListe().get(0).getId(),
            new SvpTilretteleggingDatoDto(BEHOV_DATO.plusWeeks(1), TilretteleggingType.DELVIS_TILRETTELEGGING, new BigDecimal("20.00"), new BigDecimal("40.00")));
        AksjonspunktOppdaterParameter param = new AksjonspunktOppdaterParameter(behandling, Optional.empty(), dto);

        expectedException.expect(FunksjonellException.class);
        expectedException.expectMessage("FP-682319");

        oppdaterer.oppdater(dto, param);
    }

    @Test
    public void stillingsprosent_skal_kunne_være_null_når_arbeidsforholdet_ikke_skal_brukes() {
        var behandling = behandlingMedTilretteleggingAP();

        var svpGrunnlag = byggSøknadsgrunnlag(behandling);
        var dto = byggDto(BEHOV_DATO, TERMINDATO,
            svpGrunnlag.getOpprinneligeTilrettelegginger().getTilretteleggingListe().get(0).getId(),
            false,
            new SvpTilretteleggingDatoDto(BEHOV_DATO.plusWeeks(1), TilretteleggingType.DELVIS_TILRETTELEGGING, null));

        var param = new AksjonspunktOppdaterParameter(behandling, Optional.empty(), dto);
        assertThatCode(() -> oppdaterer.oppdater(dto, param)).doesNotThrowAnyException();
    }

    private Behandling behandlingMedTilretteleggingAP() {
        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.VURDER_SVP_TILRETTELEGGING, BehandlingStegType.VURDER_TILRETTELEGGING);
        scenario.medDefaultBekreftetTerminbekreftelse();
        return scenario.lagre(repositoryProvider);
    }


    private void settOppTilgangTilOverstyring(boolean kanOverstyre) {
        var dto = new InnloggetNavAnsattDto.Builder()
            .setBrukernavn("mrOverstyrer")
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
        SvpTilretteleggingEntitet tilrettelegging = new SvpTilretteleggingEntitet.Builder()
            .medBehovForTilretteleggingFom(BEHOV_DATO)
            .medIngenTilrettelegging(BEHOV_DATO)
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .medArbeidsgiver(Arbeidsgiver.person(AktørId.dummy()))
            .medMottattTidspunkt(LocalDateTime.now())
            .medKopiertFraTidligereBehandling(false)
            .medTilretteleggingFom(new TilretteleggingFOM.Builder().medFomDato(BEHOV_DATO).medTilretteleggingType(TilretteleggingType.INGEN_TILRETTELEGGING).build())
            .build();
        SvpGrunnlagEntitet svpGrunnlag = new SvpGrunnlagEntitet.Builder()
            .medBehandlingId(behandling.getId())
            .medOpprinneligeTilrettelegginger(List.of(tilrettelegging))
            .build();
        svangerskapspengerRepository.lagreOgFlush(svpGrunnlag);
        return svpGrunnlag;
    }

    private BekreftSvangerskapspengerDto byggDto(LocalDate behovDato, LocalDate termindato, Long id, SvpTilretteleggingDatoDto... tilretteleggingDatoer) {
        return byggDto(behovDato, termindato, id, true, tilretteleggingDatoer);
    }

    private BekreftSvangerskapspengerDto byggDto(LocalDate behovDato,
                                                 LocalDate termindato,
                                                 Long id,
                                                 boolean skalBrukes,
                                                 SvpTilretteleggingDatoDto... tilretteleggingDatoer) {
        BekreftSvangerskapspengerDto dto = new BekreftSvangerskapspengerDto("Velbegrunnet begrunnelse");
        dto.setTermindato(termindato);

        SvpArbeidsforholdDto arbeidsforholdDto = new SvpArbeidsforholdDto();
        arbeidsforholdDto.setTilretteleggingBehovFom(behovDato);
        List<SvpTilretteleggingDatoDto> datoer = new ArrayList<>(Arrays.asList(tilretteleggingDatoer));

        arbeidsforholdDto.setTilretteleggingDatoer(datoer);
        arbeidsforholdDto.setArbeidsgiverNavn("Byggmaker Bob");
        arbeidsforholdDto.setMottattTidspunkt(LocalDateTime.now());
        arbeidsforholdDto.setTilretteleggingId(id);
        arbeidsforholdDto.setSkalBrukes(skalBrukes);

        dto.setBekreftetSvpArbeidsforholdList(List.of(arbeidsforholdDto));
        return dto;
    }
}
