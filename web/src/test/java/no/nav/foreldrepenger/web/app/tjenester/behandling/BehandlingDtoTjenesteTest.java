package no.nav.foreldrepenger.web.app.tjenester.behandling;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.RelatertBehandlingTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.dokument.BehandlingDokumentRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.tilbakekreving.TilbakekrevingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.tilbakekreving.TilbakekrevingValg;
import no.nav.foreldrepenger.behandlingslager.behandling.tilbakekreving.TilbakekrevingVidereBehandling;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerSvangerskapspenger;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;
import no.nav.foreldrepenger.domene.opptjening.aksjonspunkt.OpptjeningIUtlandDokStatusTjeneste;
import no.nav.foreldrepenger.domene.prosess.HentOgLagreBeregningsgrunnlagTjeneste;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.foreldrepenger.web.app.rest.ResourceLink;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.UuidDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.behandling.BehandlingDtoTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.behandling.UtvidetBehandlingDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.tilbakekreving.TilbakekrevingRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.app.KontrollerAktivitetskravDtoTjeneste;
import no.nav.foreldrepenger.web.app.util.RestUtils;

@CdiDbAwareTest
public class BehandlingDtoTjenesteTest {

    @Inject
    private BehandlingRepositoryProvider repositoryProvider;

    @Inject
    private HentOgLagreBeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste;

    @Inject
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;

    @Inject
    private TilbakekrevingRepository tilbakekrevingRepository;

    @Inject
    private OpptjeningIUtlandDokStatusTjeneste opptjeningIUtlandDokStatusTjeneste;

    @Inject
    private BehandlingDokumentRepository behandlingDokumentRepository;

    @Inject
    private RelatertBehandlingTjeneste relatertBehandlingTjeneste;

    @Inject
    private ForeldrepengerUttakTjeneste foreldrepengerUttakTjeneste;

    @Inject
    private KontrollerAktivitetskravDtoTjeneste kontrollerAktivitetskravDtoTjeneste;

    private BehandlingDtoTjeneste tjeneste;

    private LocalDate now = LocalDate.now();

    private Collection<ResourceLink> existingRoutes;

    @BeforeEach
    public void setUp() {
        existingRoutes = RestUtils.getRoutes();
        tjeneste = new BehandlingDtoTjeneste(repositoryProvider, beregningsgrunnlagTjeneste, tilbakekrevingRepository, skjæringstidspunktTjeneste,
                opptjeningIUtlandDokStatusTjeneste, behandlingDokumentRepository, relatertBehandlingTjeneste, foreldrepengerUttakTjeneste, null,
                kontrollerAktivitetskravDtoTjeneste);
    }

    @Test
    public void skal_ha_med_simuleringsresultatURL() {
        var behandling = lagBehandling();

        var dto = tjeneste.lagUtvidetBehandlingDto(behandling, null);

        assertThat(getLinkRel(dto)).contains("simuleringResultat");
        assertThat(getLinkHref(dto)).contains(URI.create("/fpoppdrag/api/simulering/resultat-uten-inntrekk"));
    }

    @Test
    public void skal_ha_med_tilbakekrevings_link_når_det_finnes_et_resultat() {
        var behandling = lagBehandling();

        tilbakekrevingRepository.lagre(behandling,
                TilbakekrevingValg.utenMulighetForInntrekk(TilbakekrevingVidereBehandling.TILBAKEKREV_I_INFOTRYGD, "varsel"));

        var dto = tjeneste.lagUtvidetBehandlingDto(behandling, null);
        var href = RestUtils.getApiPath(TilbakekrevingRestTjeneste.VALG_PATH);
        var link = ResourceLink.get(href, "", new UuidDto(dto.getUuid()));
        assertThat(getLinkRel(dto)).contains("tilbakekrevingvalg");
        assertThat(getLinkHref(dto)).contains(link.getHref());
    }

    @Test
    public void skal_ikke_ha_med_tilbakekrevings_link_når_det_ikke_finnes_et_resultat() {
        var behandling = lagBehandling();

        var dto = tjeneste.lagUtvidetBehandlingDto(behandling, null);
        var href = RestUtils.getApiPath(TilbakekrevingRestTjeneste.VALG_PATH);
        var link = ResourceLink.get(href, "", new UuidDto(dto.getUuid()));
        assertThat(getLinkRel(dto)).doesNotContain("tilbakekrevingvalg");
        assertThat(getLinkHref(dto)).doesNotContain(link.getHref());
    }

    @Test
    public void alle_paths_skal_eksistere() {
        Set<Behandling> behandlinger = new HashSet<>();
        behandlinger.add(lagBehandling());
        behandlinger.add(lagBehandling(BehandlingType.KLAGE));
        behandlinger.add(lagBehandling(BehandlingType.ANKE));
        behandlinger.add(lagBehandling(BehandlingType.INNSYN));
        behandlinger.add(lagBehandling(FagsakYtelseType.ENGANGSTØNAD));
        behandlinger.add(lagBehandling(FagsakYtelseType.SVANGERSKAPSPENGER));
        for (var behandling : behandlinger) {
            for (var dtoLink : tjeneste.lagUtvidetBehandlingDto(behandling, null).getLinks()) {
                assertThat(routeExists(dtoLink)).withFailMessage("Route " + dtoLink.toString() + " does not exist.").isTrue();
            }
        }
    }

    private Boolean routeExists(ResourceLink dtoLink) {
        Boolean linkEksists = false;
        if (dtoLink.getRel().equals("simuleringResultat")) {
            return true;
        }
        for (var routeLink : existingRoutes) {
            if (dtoLink.getHref().getPath().equals(routeLink.getHref().getPath()) && dtoLink.getType().equals(routeLink.getType())) {
                linkEksists = true;
                break;
            }
        }
        return linkEksists;
    }

    private Behandling lagBehandling() {
        return ScenarioMorSøkerForeldrepenger.forFødsel()
                .medDefaultSøknadTerminbekreftelse()
                .medDefaultBekreftetTerminbekreftelse()
                .medFordeling(new OppgittFordelingEntitet(Collections.singletonList(OppgittPeriodeBuilder.ny()
                        .medPeriodeType(UttakPeriodeType.FEDREKVOTE)
                        .medPeriode(now.plusWeeks(8), now.plusWeeks(12))
                        .build()), true))
                .lagre(repositoryProvider);
    }

    private Behandling lagBehandling(BehandlingType behandlingType) {
        return ScenarioMorSøkerForeldrepenger.forFødsel()
                .medDefaultSøknadTerminbekreftelse()
                .medDefaultBekreftetTerminbekreftelse()
                .medFordeling(new OppgittFordelingEntitet(Collections.singletonList(OppgittPeriodeBuilder.ny()
                        .medPeriodeType(UttakPeriodeType.FEDREKVOTE)
                        .medPeriode(now.plusWeeks(8), now.plusWeeks(12))
                        .build()), true))
                .medBehandlingType(behandlingType)
                .lagre(repositoryProvider);
    }

    private Behandling lagBehandling(FagsakYtelseType fagsakYtelseType) {
        if (fagsakYtelseType.equals(FagsakYtelseType.SVANGERSKAPSPENGER)) {
            return ScenarioMorSøkerSvangerskapspenger.forSvangerskapspenger().lagre(repositoryProvider);
        }
        if (fagsakYtelseType.equals(FagsakYtelseType.ENGANGSTØNAD)) {
            var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
            scenario.medSøknadHendelse().erFødsel().build();
            return scenario.lagre(repositoryProvider);
        }
        return null;
    }

    private List<URI> getLinkHref(UtvidetBehandlingDto dto) {
        return dto.getLinks().stream().map(ResourceLink::getHref).collect(Collectors.toList());
    }

    private List<String> getLinkRel(UtvidetBehandlingDto dto) {
        return dto.getLinks().stream().map(ResourceLink::getRel).collect(Collectors.toList());
    }
}
