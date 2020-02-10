package no.nav.foreldrepenger.web.app.tjenester.behandling.svp;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
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
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.TilretteleggingType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;
import no.nav.foreldrepenger.historikk.dto.HistorikkInnslagKonverter;
import no.nav.vedtak.exception.FunksjonellException;
import no.nav.vedtak.util.FPDateUtil;

public class BekreftSvangerskapspengerOppdatererTest {

    private static final LocalDate HEL_FOM = FPDateUtil.iDag().minusDays(1);
    private static final LocalDate DELVIS_FOM = FPDateUtil.iDag();
    private static final LocalDate INGEN_FOM = FPDateUtil.iDag().plusDays(1);

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

    private BekreftSvangerskapspengerOppdaterer oppdaterer = new BekreftSvangerskapspengerOppdaterer(svangerskapspengerRepository, historikkAdapter, repositoryProvider, familieHendelseRepository);

    @Test
    public void skal_sette_totrinn_ved_endring() {
        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.VURDER_SVP_TILRETTELEGGING, BehandlingStegType.VURDER_TILRETTELEGGING);
        scenario.medDefaultBekreftetTerminbekreftelse();
        Behandling behandling = scenario.lagre(repositoryProvider);

        SvpGrunnlagEntitet svpGrunnlag = byggSøknadsgrunnlag(behandling, INGEN_FOM.plusDays(2));
        BekreftSvangerskapspengerDto dto = byggDto(HEL_FOM, DELVIS_FOM, INGEN_FOM, FPDateUtil.iDag().plusDays(40), svpGrunnlag.getOpprinneligeTilrettelegginger().getTilretteleggingListe().get(0).getId());
        AksjonspunktOppdaterParameter param = new AksjonspunktOppdaterParameter(behandling, Optional.empty(), dto);

        OppdateringResultat resultat = oppdaterer.oppdater(dto, param);

        assertThat(resultat.kreverTotrinnsKontroll()).isTrue();
    }

    @Test
    public void skal_feile_ved_like_tilretteleggingsdatoer() {
        BekreftSvangerskapspengerDto dto = byggDto(FPDateUtil.iDag(), FPDateUtil.iDag(), FPDateUtil.iDag().plusDays(1), FPDateUtil.iDag().plusMonths(2), 111L);

        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.VURDER_SVP_TILRETTELEGGING, BehandlingStegType.VURDER_TILRETTELEGGING);
        Behandling behandling = scenario.lagre(repositoryProvider);
        AksjonspunktOppdaterParameter param = new AksjonspunktOppdaterParameter(behandling, Optional.empty(), dto);

        expectedException.expect(FunksjonellException.class);
        expectedException.expectMessage("FP-682318");

        oppdaterer.oppdater(dto, param);

    }

    private SvpGrunnlagEntitet byggSøknadsgrunnlag(Behandling behandling, LocalDate ingenFom) {
        SvpTilretteleggingEntitet tilrettelegging = new SvpTilretteleggingEntitet.Builder()
            .medBehovForTilretteleggingFom(ingenFom)
            .medIngenTilrettelegging(ingenFom)
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .medArbeidsgiver(Arbeidsgiver.person(AktørId.dummy()))
            .medMottattTidspunkt(FPDateUtil.nå())
            .medKopiertFraTidligereBehandling(false)
            .build();
        SvpGrunnlagEntitet svpGrunnlag = new SvpGrunnlagEntitet.Builder()
            .medBehandlingId(behandling.getId())
            .medOpprinneligeTilrettelegginger(List.of(tilrettelegging))
            .build();
        svangerskapspengerRepository.lagreOgFlush(svpGrunnlag);
        return svpGrunnlag;
    }

    private BekreftSvangerskapspengerDto byggDto(LocalDate helFom, LocalDate delvisFom, LocalDate ingenFom, LocalDate termindato, Long id) {
        BekreftSvangerskapspengerDto dto = new BekreftSvangerskapspengerDto("Velbegrunnet begrunnelse");
        dto.setTermindato(termindato);

        SvpArbeidsforholdDto arbeidsforholdDto = new SvpArbeidsforholdDto();
        arbeidsforholdDto.setTilretteleggingBehovFom(ingenFom);
        List<SvpTilretteleggingDatoDto> datoer = new ArrayList<>();
        datoer.add(new SvpTilretteleggingDatoDto(ingenFom, TilretteleggingType.INGEN_TILRETTELEGGING, null));
        datoer.add(new SvpTilretteleggingDatoDto(delvisFom, TilretteleggingType.DELVIS_TILRETTELEGGING, BigDecimal.valueOf(80.0)));
        datoer.add(new SvpTilretteleggingDatoDto(helFom, TilretteleggingType.HEL_TILRETTELEGGING, null));

        arbeidsforholdDto.setTilretteleggingDatoer(datoer);
        arbeidsforholdDto.setArbeidsgiverNavn("Byggmaker Bob");
        arbeidsforholdDto.setMottattTidspunkt(FPDateUtil.nå());
        arbeidsforholdDto.setTilretteleggingId(id);
        arbeidsforholdDto.setSkalBrukes(true);

        dto.setBekreftetSvpArbeidsforholdList(Collections.singletonList(arbeidsforholdDto));
        return dto;
    }
}
