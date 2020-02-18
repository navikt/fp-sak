package no.nav.foreldrepenger.behandling.anke;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.anke.impl.AnkeVurderingAdapter;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeOmgjørÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeVurdering;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeVurderingOmgjør;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeVurderingResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.ProsesseringAsynkTjeneste;

@ApplicationScoped
public class AnkeVurderingTjeneste {
    private ProsesseringAsynkTjeneste prosesseringAsynkTjeneste;
    private AnkeRepository ankeRepository;
    private BehandlingRepository behandlingRepository;
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;
    private BehandlingsresultatRepository behandlingsresultatRepository;

    AnkeVurderingTjeneste() {
        // for CDI proxy
    }

    @Inject
    public AnkeVurderingTjeneste(ProsesseringAsynkTjeneste prosesseringAsynkTjeneste,
                                 BehandlingRepository behandlingRepository,
                                 BehandlingsresultatRepository behandlingsresultatRepository,
                                 AnkeRepository ankeRepository,
                                 BehandlingskontrollTjeneste behandlingskontrollTjeneste) {
        this.prosesseringAsynkTjeneste = prosesseringAsynkTjeneste;
        this.ankeRepository = ankeRepository;
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.behandlingsresultatRepository = behandlingsresultatRepository;
    }

    public void oppdater(Behandling behandling, AnkeVurderingAdapter adapter) {
        byggOgLagreAnkeVurderingResultat(behandling, adapter);
        settBehandlingResultatTypeBasertPaaUtfall(behandling, adapter);
    }

    public void mellomlagreVurderingResultat(Behandling behandling, AnkeVurderingAdapter adapter) {
        byggOgLagreAnkeVurderingResultat(behandling, adapter);
    }

    public void mellomlagreVurderingResultatOgÅpneAksjonspunkt(Behandling behandling, AnkeVurderingAdapter adapter) {
        tilbakeførBehandling(behandling);
        byggOgLagreAnkeVurderingResultat(behandling, adapter);
    }

    public void oppdaterAnkeMedPåanketBehandling(Long ankeBehandlingId, Long påanketBehandling) {
        Behandling ankeBehandling = behandlingRepository.hentBehandling(ankeBehandlingId);
        if (påanketBehandling == null) {
            ankeRepository.settPåAnketBehandling(ankeBehandling, null);
            return;
        }
        Behandling påAnketBehandling = behandlingRepository.hentBehandling(påanketBehandling);
        ankeRepository.settPåAnketBehandling(ankeBehandling, påAnketBehandling);
    }

    private void tilbakeførBehandling(Behandling behandling) {
        BehandlingskontrollKontekst kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(behandling.getId());
        behandlingskontrollTjeneste.behandlingTilbakeføringTilTidligereBehandlingSteg(kontekst, BehandlingStegType.ANKE);
        prosesseringAsynkTjeneste.asynkProsesserBehandling(behandling);
    }

    private void byggOgLagreAnkeVurderingResultat(Behandling behandling, AnkeVurderingAdapter adapter) {
        boolean gjelderVedtak = adapter.getPaaAnketBehandlingId() != null;
        if (gjelderVedtak) {
            oppdaterAnkeMedPåanketBehandling(behandling.getId(), adapter.getPaaAnketBehandlingId());
        }

        AnkeVurdering ankeVurdering = adapter.getAnkeVurderingKode() != null ? AnkeVurdering.fraKode(adapter.getAnkeVurderingKode())
            : null;
        Optional<AnkeResultatEntitet> ankeResultat = ankeRepository.hentAnkeResultat(behandling);
        AnkeVurderingResultatEntitet.Builder ankeVurderingResultatBuilder = new AnkeVurderingResultatEntitet.Builder()
            .medBegrunnelse(adapter.getBegrunnelse())
            .medFritekstTilBrev(adapter.getFritekstTilBrev())
            .medAnkeVurdering(ankeVurdering)
            .medGjelderVedtak(gjelderVedtak)
            .medErAnkerIkkePart(adapter.erAnkerIkkePart())
            .medErFristIkkeOverholdt(adapter.erFristIkkeOverholdt())
            .medErIkkeKonkret(adapter.erIkkeKonkret())
            .medErIkkeSignert(adapter.erIkkeSignert())
            .medErSubsidiartRealitetsbehandles(adapter.getErSubsidiartRealitetsbehandles())
            .medGodkjentAvMedunderskriver(adapter.getErGodkjentAvMedunderskriver())
            .medMerknaderFraBruker(adapter.getMerknaderFraBruker())
            .medErMerknaderMottatt(adapter.erMerknaderMottatt());

        ankeResultat.ifPresent(ankeVurderingResultatBuilder::medAnkeResultat);

        Optional<String> ankeOmgjørÅrsak = adapter.getAnkeOmgjoerArsakKode();
        ankeOmgjørÅrsak.ifPresent(omgjørÅrsak -> ankeVurderingResultatBuilder
            .medAnkeOmgjørÅrsak(AnkeOmgjørÅrsak.fraKode(omgjørÅrsak)));

        Optional<String> ankeVurderingOmgjør = adapter.getAnkeVurderingOmgjoer();
        ankeVurderingOmgjør.ifPresent(vurderingOmgjør -> ankeVurderingResultatBuilder
            .medAnkeVurderingOmgjør(AnkeVurderingOmgjør.fraKode(vurderingOmgjør)));

        ankeVurderingResultatBuilder.medGodkjentAvMedunderskriver(erGodkjentAvMedunderskriver(behandling, ankeVurderingResultatBuilder.build()));
        ankeRepository.lagreVurderingsResultat(behandling, ankeVurderingResultatBuilder);
    }

    private boolean erGodkjentAvMedunderskriver(Behandling behandling, AnkeVurderingResultatEntitet ankeVurderingResultat) {
        Optional<AnkeVurderingResultatEntitet> gammeltAnkeVurderingResultat = ankeRepository.hentAnkeVurderingResultat(behandling.getId());
        return gammeltAnkeVurderingResultat.isPresent() && gammeltAnkeVurderingResultat.get().godkjentAvMedunderskriver()
            && gammeltAnkeVurderingResultat.get().getAnkeVurdering().equals(ankeVurderingResultat.getAnkeVurdering())
            && gammeltAnkeVurderingResultat.get().getAnkeOmgjørÅrsak().equals(ankeVurderingResultat.getAnkeOmgjørÅrsak())
            && gammeltAnkeVurderingResultat.get().getAnkeVurderingOmgjør().equals(ankeVurderingResultat.getAnkeVurderingOmgjør())
            && gammeltAnkeVurderingResultat.get().erAnkerIkkePart() == ankeVurderingResultat.erAnkerIkkePart()
            && gammeltAnkeVurderingResultat.get().erFristIkkeOverholdt() == ankeVurderingResultat.erFristIkkeOverholdt()
            && gammeltAnkeVurderingResultat.get().erIkkeKonkret() == ankeVurderingResultat.erIkkeKonkret()
            && gammeltAnkeVurderingResultat.get().erIkkeSignert() == ankeVurderingResultat.erIkkeSignert()
            && gammeltAnkeVurderingResultat.get().erSubsidiartRealitetsbehandles() == ankeVurderingResultat.erSubsidiartRealitetsbehandles()
            && gammeltAnkeVurderingResultat.get().getFritekstTilBrev().equals(ankeVurderingResultat.getFritekstTilBrev())
            && (  (gammeltAnkeVurderingResultat.get().getBegrunnelse() == null && ankeVurderingResultat.getBegrunnelse() == null) ||
                  (gammeltAnkeVurderingResultat.get().getBegrunnelse() != null  && gammeltAnkeVurderingResultat.get().getBegrunnelse().equals(ankeVurderingResultat.getBegrunnelse())) )
            && gammeltAnkeVurderingResultat.get().getGjelderVedtak() == ankeVurderingResultat.getGjelderVedtak();
    }

    private void settBehandlingResultatTypeBasertPaaUtfall(Behandling behandling, AnkeVurderingAdapter adapter) {
        AnkeVurdering ankeVurdering = AnkeVurdering.fraKode(adapter.getAnkeVurderingKode());
        Optional<Behandlingsresultat> behandlingsresultat = behandlingsresultatRepository.hentHvisEksisterer(behandling.getId());

        if (behandlingsresultat.isEmpty()) {
            behandlingsresultat = Optional.ofNullable(Behandlingsresultat.opprettFor(behandling));
        }
        Behandlingsresultat.builderEndreEksisterende(behandlingsresultat.get())
            .medBehandlingResultatType(BehandlingResultatType.tolkBehandlingResultatType(ankeVurdering));

    }
}
