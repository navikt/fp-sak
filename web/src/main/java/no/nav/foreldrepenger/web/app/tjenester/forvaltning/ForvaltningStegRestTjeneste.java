package no.nav.foreldrepenger.web.app.tjenester.forvaltning;

import static no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType.KONTROLLERER_SØKERS_OPPLYSNINGSPLIKT;
import static no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType.KONTROLLER_FAKTA;
import static no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType.KONTROLLER_FAKTA_ARBEIDSFORHOLD;
import static no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType.FØRSTEGANGSSØKNAD;
import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;

import java.util.Optional;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.swagger.v3.oas.annotations.Operation;
import no.nav.foreldrepenger.abac.FPSakBeskyttetRessursAttributt;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat.Builder;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektsmeldingTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.impl.ArbeidsforholdAdministrasjonTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.Inntektsmelding;
import no.nav.foreldrepenger.domene.typer.JournalpostId;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;
import no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt.BehandlingsprosessApplikasjonTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto.ForvaltningBehandlingIdDto;
import no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto.HoppTilbakeDto;
import no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto.HoppTilbakeTil5080OgSlettInntektsmeldingDto;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;

@Path("/forvaltningSteg")
@ApplicationScoped
@Transactional
public class ForvaltningStegRestTjeneste {

    private BehandlingsprosessApplikasjonTjeneste behandlingsprosessTjeneste;
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;
    private ArbeidsforholdAdministrasjonTjeneste arbeidsforholdAdministrasjonTjeneste;
    private HistorikkRepository historikkRepository;
    private FamilieHendelseRepository familieHendelseRepository;
    private InntektsmeldingTjeneste inntektsmeldingTjeneste;
    private VilkårResultatRepository vilkårResultatRepository;

    @Inject
    public ForvaltningStegRestTjeneste(BehandlingsprosessApplikasjonTjeneste behandlingsprosessTjeneste,
            BehandlingskontrollTjeneste behandlingskontrollTjeneste,
            ArbeidsforholdAdministrasjonTjeneste arbeidsforholdAdministrasjonTjeneste,
            InntektsmeldingTjeneste inntektsmeldingTjeneste,
            BehandlingRepositoryProvider repositoryProvider, VilkårResultatRepository vilkårResultatRepository) {
        this.behandlingsprosessTjeneste = behandlingsprosessTjeneste;
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
        this.arbeidsforholdAdministrasjonTjeneste = arbeidsforholdAdministrasjonTjeneste;
        this.inntektsmeldingTjeneste = inntektsmeldingTjeneste;
        this.historikkRepository = repositoryProvider.getHistorikkRepository();
        this.familieHendelseRepository = repositoryProvider.getFamilieHendelseRepository();
        this.vilkårResultatRepository = vilkårResultatRepository;
    }

    public ForvaltningStegRestTjeneste() {
        // CDI
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Generelt tilbakehopp", tags = "FORVALTNING-steg-hopp")
    @Path("/generell")
    @BeskyttetRessurs(action = READ, resource = FPSakBeskyttetRessursAttributt.DRIFT, sporingslogg = false)
    public Response hoppTilbake(@BeanParam @Valid HoppTilbakeDto dto) {
        Long behandlingId = dto.getBehandlingId();
        String behandlingStegTypeStr = dto.getBehandlingStegType();
        BehandlingStegType tilSteg = BehandlingStegType.fraKode(behandlingStegTypeStr);

        hoppTilbake(behandlingId, tilSteg);

        return Response.ok().build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Hopp tilbake til 5080", tags = "FORVALTNING-steg-hopp")
    @Path("/5080")
    @BeskyttetRessurs(action = READ, resource = FPSakBeskyttetRessursAttributt.DRIFT, sporingslogg = false)
    public Response hoppTilbakeTil5080(@BeanParam @Valid ForvaltningBehandlingIdDto dto) {
        Long behandlingId = dto.getBehandlingId();

        hoppTilbake(behandlingId, KONTROLLER_FAKTA_ARBEIDSFORHOLD);

        return Response.ok().build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Hopp tilbake til 5080 og fjern OPPTJENINGSVILKÅRET", tags = "FORVALTNING-steg-hopp")
    @Path("/fjern-opptjeningsvilkåret")
    @BeskyttetRessurs(action = READ, resource = FPSakBeskyttetRessursAttributt.DRIFT, sporingslogg = false)
    public Response hoppTilbakeTil5080OgFjernOverstyringAvOpptjening(@BeanParam @Valid ForvaltningBehandlingIdDto dto) {
        Long behandlingId = dto.getBehandlingId();

        Optional<VilkårResultat> vilkårResultatOpt = vilkårResultatRepository.hentHvisEksisterer(behandlingId);

        if (vilkårResultatOpt.isPresent()) {
            VilkårResultat vilkårResultat = vilkårResultatOpt.get();
            Optional<Vilkår> opptjeningsvilkåretOpt = vilkårResultat.getVilkårene()
                    .stream()
                    .filter(v -> v.getVilkårType() == VilkårType.OPPTJENINGSVILKÅRET)
                    .filter(Vilkår::erOverstyrt)
                    .findFirst();

            if (opptjeningsvilkåretOpt.isPresent()) {
                Builder builder = VilkårResultat.builderFraEksisterende(vilkårResultat);
                builder.fjernVilkår(VilkårType.OPPTJENINGSVILKÅRET);

                VilkårResultat nyttVilkårResulatat = builder.build();
                vilkårResultatRepository.lagre(behandlingId, nyttVilkårResulatat);

                Behandling behandling = behandlingsprosessTjeneste.hentBehandling(behandlingId);
                if (behandling.erRevurdering()) {
                    hoppTilbake(behandlingId, KONTROLLERER_SØKERS_OPPLYSNINGSPLIKT);
                } else if (behandling.getType() == FØRSTEGANGSSØKNAD) {
                    hoppTilbake(behandlingId, KONTROLLER_FAKTA_ARBEIDSFORHOLD);
                }
            }
        }
        return Response.ok().build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Hopp tilbake til 5080 og slett inntektsmelding", tags = "FORVALTNING-steg-hopp")
    @Path("/inntektsmelding")
    @BeskyttetRessurs(action = READ, resource = FPSakBeskyttetRessursAttributt.DRIFT, sporingslogg = false)
    public Response hoppTilbakeTil5080OgSlettInntektsmelding(@BeanParam @Valid HoppTilbakeTil5080OgSlettInntektsmeldingDto dto) {
        Long behandlingId = dto.getBehandlingId();
        var journalpostId = new JournalpostId(Long.parseLong(dto.getJournalpostId().trim()));
        Optional<Inntektsmelding> inntektsmelding = inntektsmeldingTjeneste.hentInntektsMeldingFor(behandlingId, journalpostId);
        if (inntektsmelding.isPresent()) {
            inntektsmeldingTjeneste.fjernInntektsmelding(behandlingId, Set.of(journalpostId));
            hoppTilbake(behandlingId, KONTROLLER_FAKTA_ARBEIDSFORHOLD);
            return Response.ok().build();
        }
        return Response.noContent().build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Fjerner overstyring av familiehendelse og hopper tilbake til KOFAK", tags = "FORVALTNING-steg-hopp")
    @Path("/fjernFHValgHoppTilbake")
    @BeskyttetRessurs(action = READ, resource = FPSakBeskyttetRessursAttributt.DRIFT, sporingslogg = false)
    public Response fjernOverstyrtFH(@BeanParam @Valid ForvaltningBehandlingIdDto dto) {
        Long behandlingId = dto.getBehandlingId();
        FamilieHendelseGrunnlagEntitet grunnlag = familieHendelseRepository.hentAggregat(behandlingId);
        if (grunnlag.getOverstyrtVersjon().isPresent()) {
            Behandling behandling = behandlingsprosessTjeneste.hentBehandling(behandlingId);
            BehandlingskontrollKontekst kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(behandling);
            familieHendelseRepository.slettAvklarteData(behandlingId, kontekst.getSkriveLås());
            doHoppTilSteg(behandling, kontekst, KONTROLLER_FAKTA);
            behandlingsprosessTjeneste.gjenopptaBehandling(behandling);
            return Response.ok().build();
        }
        return Response.noContent().build();
    }

    private void hoppTilbake(Long behandlingId, BehandlingStegType tilSteg) {
        Behandling behandling = behandlingsprosessTjeneste.hentBehandling(behandlingId);
        BehandlingskontrollKontekst kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(behandling);
        if (KONTROLLER_FAKTA_ARBEIDSFORHOLD.equals(tilSteg)) {
            arbeidsforholdAdministrasjonTjeneste.fjernOverstyringerGjortAvSaksbehandler(behandling.getId(), behandling.getAktørId());
        }
        doHoppTilSteg(behandling, kontekst, tilSteg);
        if (behandling.isBehandlingPåVent()) {
            behandlingskontrollTjeneste.taBehandlingAvVentSetAlleAutopunktUtført(behandling, kontekst);
        }
        behandlingsprosessTjeneste.asynkKjørProsess(behandling);
    }

    private void doHoppTilSteg(Behandling behandling, BehandlingskontrollKontekst kontekst, BehandlingStegType tilSteg) {
        behandlingskontrollTjeneste.taBehandlingAvVentSetAlleAutopunktUtført(behandling, kontekst);
        lagHistorikkinnslag(behandling, tilSteg.getNavn());

        behandlingskontrollTjeneste.behandlingTilbakeføringTilTidligereBehandlingSteg(kontekst, tilSteg);
    }

    private void lagHistorikkinnslag(Behandling behandling, String tilStegNavn) {
        Historikkinnslag nyeRegisteropplysningerInnslag = new Historikkinnslag();
        nyeRegisteropplysningerInnslag.setAktør(HistorikkAktør.VEDTAKSLØSNINGEN);
        nyeRegisteropplysningerInnslag.setType(HistorikkinnslagType.SPOLT_TILBAKE);
        nyeRegisteropplysningerInnslag.setBehandlingId(behandling.getId());

        String fraStegNavn = behandling.getAktivtBehandlingSteg() != null ? behandling.getAktivtBehandlingSteg().getNavn() : null;
        HistorikkInnslagTekstBuilder historieBuilder = new HistorikkInnslagTekstBuilder()
                .medHendelse(HistorikkinnslagType.SPOLT_TILBAKE)
                .medBegrunnelse("Behandlingen er flyttet fra " + fraStegNavn + " tilbake til " + tilStegNavn);
        historieBuilder.build(nyeRegisteropplysningerInnslag);
        historikkRepository.lagre(nyeRegisteropplysningerInnslag);
    }
}
