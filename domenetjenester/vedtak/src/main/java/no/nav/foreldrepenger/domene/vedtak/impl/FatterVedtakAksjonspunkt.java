package no.nav.foreldrepenger.domene.vedtak.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.VurderÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeVurderingResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurderingResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurdertAv;
import no.nav.foreldrepenger.domene.vedtak.VedtakAksjonspunktData;
import no.nav.foreldrepenger.domene.vedtak.VedtakTjeneste;
import no.nav.foreldrepenger.produksjonsstyring.totrinn.TotrinnTjeneste;
import no.nav.foreldrepenger.produksjonsstyring.totrinn.Totrinnsvurdering;
import no.nav.vedtak.sikkerhet.context.SubjectHandler;

@ApplicationScoped
public class FatterVedtakAksjonspunkt {

    private KlageRepository klageRepository;
    private VedtakTjeneste vedtakTjeneste;
    private TotrinnTjeneste totrinnTjeneste;
    private AnkeRepository ankeRepository;
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;

    public FatterVedtakAksjonspunkt() {
    }

    @Inject
    public FatterVedtakAksjonspunkt(BehandlingskontrollTjeneste behandlingskontrollTjeneste,
                                    KlageRepository klageRepository,
                                    AnkeRepository ankeRepository,
                                    VedtakTjeneste vedtakTjeneste,
                                    TotrinnTjeneste totrinnTjeneste) {
        this.klageRepository = klageRepository;
        this.vedtakTjeneste = vedtakTjeneste;
        this.totrinnTjeneste = totrinnTjeneste;
        this.ankeRepository = ankeRepository;
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
    }

    public void oppdater(Behandling behandling, Collection<VedtakAksjonspunktData> aksjonspunkter) {
        BehandlingskontrollKontekst kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(behandling);
        behandling.setAnsvarligBeslutter(SubjectHandler.getSubjectHandler().getUid());

        List<Totrinnsvurdering> totrinnsvurderinger = new ArrayList<>();
        List<Aksjonspunkt> skalReåpnes = new ArrayList<>();

        for (VedtakAksjonspunktData aks : aksjonspunkter) {
            boolean erTotrinnGodkjent = aks.isGodkjent();
            Aksjonspunkt aksjonspunkt = behandling.getAksjonspunktFor(aks.getAksjonspunktDefinisjon());
            if (!aks.isGodkjent()) {
                skalReåpnes.add(aksjonspunkt);
            }

            if (BehandlingType.KLAGE.equals(behandling.getType())) {
                erTotrinnGodkjent = erKlageGodkjentHosMedunderskriver(behandling, aks.isGodkjent());
            }
            if (BehandlingType.ANKE.equals(behandling.getType())) {
                erTotrinnGodkjent = erAnkeGodkjentHosMedunderskriver(behandling, aks.isGodkjent());
            }
            Set<String> koder = aks.getVurderÅrsakskoder();
            Collection<VurderÅrsak> vurderÅrsaker = koder.stream().map(VurderÅrsak::fraKode).collect(Collectors.toSet());

            Totrinnsvurdering.Builder vurderingBuilder = new Totrinnsvurdering.Builder(behandling, aks.getAksjonspunktDefinisjon());
            vurderingBuilder.medGodkjent(erTotrinnGodkjent);
            vurderÅrsaker.forEach(vurderingBuilder::medVurderÅrsak);
            vurderingBuilder.medBegrunnelse(aks.getBegrunnelse());
            totrinnsvurderinger.add(vurderingBuilder.build());
        }
        totrinnTjeneste.settNyeTotrinnaksjonspunktvurderinger(behandling, totrinnsvurderinger);
        vedtakTjeneste.lagHistorikkinnslagFattVedtak(behandling);
        // Noe spesialhåndtering ifm totrinn og tilbakeføring fra FVED
        behandlingskontrollTjeneste.lagreAksjonspunkterReåpnet(kontekst, skalReåpnes, false, true);
    }

    private boolean erKlageGodkjentHosMedunderskriver(Behandling behandling, boolean erAksjonspunktGodkjent) {
        Optional<KlageVurderingResultat> klageVurderingResultat = klageRepository.hentGjeldendeKlageVurderingResultat(behandling);
        if (klageVurderingResultat.isPresent() && klageVurderingResultat.get().getKlageVurdertAv().equals(KlageVurdertAv.NK) && erAksjonspunktGodkjent) {
            KlageVurderingResultat.Builder builder = new KlageVurderingResultat.Builder()
                .medGodkjentAvMedunderskriver(true)
                .medKlageVurdertAv(klageVurderingResultat.get().getKlageVurdertAv())
                .medKlageResultat(klageVurderingResultat.get().getKlageResultat())
                .medKlageVurdering(klageVurderingResultat.get().getKlageVurdering())
                .medFritekstTilBrev(klageVurderingResultat.get().getFritekstTilBrev())
                .medBegrunnelse(klageVurderingResultat.get().getBegrunnelse())
                .medKlageMedholdÅrsak(klageVurderingResultat.get().getKlageMedholdÅrsak())
                .medKlageVurderingOmgjør(klageVurderingResultat.get().getKlageVurderingOmgjør());
            klageRepository.lagreVurderingsResultat(behandling, builder);
            // Dette er spesialbehandling av klage hos KA.
            // Hvis KAs medunderskriver(belsutter) godkjenner behandlingen må den fremdeles tilbake til saksbehandler
            // så han/hun kan ferdigstille behandlingen derfor returneres det false her.
            return false;
        }
        return erAksjonspunktGodkjent;
    }

    private boolean erAnkeGodkjentHosMedunderskriver(Behandling behandling, boolean erAksjonspunktGodkjent) {
        Optional<AnkeVurderingResultatEntitet> ankeVurderingResultat = ankeRepository.hentAnkeVurderingResultat(behandling.getId());
        if (ankeVurderingResultat.isPresent() && erAksjonspunktGodkjent) {
            AnkeVurderingResultatEntitet.Builder builder = new AnkeVurderingResultatEntitet.Builder()
                .medGodkjentAvMedunderskriver(true)
                .medAnkeResultat(ankeVurderingResultat.get().getAnkeResultat())
                .medAnkeVurdering(ankeVurderingResultat.get().getAnkeVurdering())
                .medFritekstTilBrev(ankeVurderingResultat.get().getFritekstTilBrev())
                .medBegrunnelse(ankeVurderingResultat.get().getBegrunnelse())
                .medAnkeOmgjørÅrsak(ankeVurderingResultat.get().getAnkeOmgjørÅrsak())
                .medAnkeVurderingOmgjør(ankeVurderingResultat.get().getAnkeVurderingOmgjør())
                .medErAnkerIkkePart(ankeVurderingResultat.get().erAnkerIkkePart())
                .medErFristIkkeOverholdt(ankeVurderingResultat.get().erFristIkkeOverholdt())
                .medErIkkeKonkret(ankeVurderingResultat.get().erIkkeKonkret())
                .medErIkkeSignert(ankeVurderingResultat.get().erIkkeSignert())
                .medErSubsidiartRealitetsbehandles(ankeVurderingResultat.get().erSubsidiartRealitetsbehandles())
                .medGjelderVedtak(ankeVurderingResultat.get().getGjelderVedtak());
            ankeRepository.lagreVurderingsResultat(behandling, builder);
            // Dette er spesialbehandling av anke hos KA.
            // Hvis KAs medunderskriver(belsutter) godkjenner behandlingen må den fremdeles tilbake til saksbehandler
            // så han/hun kan ferdigstille behandlingen derfor returneres det false her.
            return false;
        }
        return erAksjonspunktGodkjent;
    }
}
