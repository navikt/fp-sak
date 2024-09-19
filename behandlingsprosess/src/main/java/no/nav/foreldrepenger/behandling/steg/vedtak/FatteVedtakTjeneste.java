package no.nav.foreldrepenger.behandling.steg.vedtak;

import static java.lang.Boolean.TRUE;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.totrinn.Totrinnsvurdering;
import no.nav.foreldrepenger.behandlingslager.lagretvedtak.LagretVedtak;
import no.nav.foreldrepenger.behandlingslager.lagretvedtak.LagretVedtakRepository;
import no.nav.foreldrepenger.datavarehus.xml.FatteVedtakXmlTjeneste;
import no.nav.foreldrepenger.domene.vedtak.TotrinnTjeneste;
import no.nav.foreldrepenger.domene.vedtak.VedtakTjeneste;
import no.nav.foreldrepenger.domene.vedtak.impl.KlageAnkeVedtakTjeneste;
import no.nav.foreldrepenger.konfig.Environment;
import no.nav.vedtak.exception.TekniskException;

@ApplicationScoped
public class FatteVedtakTjeneste {

    private static final Environment ENV = Environment.current(); // TODO medlemskap2 standardisere etter omlegging

    private static final Set<AksjonspunktDefinisjon> LEGACY_MEDLEM = Set.of(AksjonspunktDefinisjon.AVKLAR_LOVLIG_OPPHOLD,
        AksjonspunktDefinisjon.AVKLAR_OM_ER_BOSATT, AksjonspunktDefinisjon.AVKLAR_GYLDIG_MEDLEMSKAPSPERIODE, AksjonspunktDefinisjon.AVKLAR_OPPHOLDSRETT,
        AksjonspunktDefinisjon.AVKLAR_FORTSATT_MEDLEMSKAP);

    private static final String FPSAK_IMAGE = Environment.current().imageName();

    public static final String UTVIKLER_FEIL_VEDTAK = "Utvikler-feil: Vedtak kan ikke fattes, behandlingsresultat er ";

    private static final Set<BehandlingResultatType> VEDTAKSTILSTANDER_REVURDERING = Set.of(BehandlingResultatType.AVSLÅTT,
            BehandlingResultatType.INNVILGET, BehandlingResultatType.FORELDREPENGER_SENERE,
            BehandlingResultatType.OPPHØR, BehandlingResultatType.FORELDREPENGER_ENDRET, BehandlingResultatType.INGEN_ENDRING);
    private static final Set<BehandlingResultatType> VEDTAKSTILSTANDER = Set.of(BehandlingResultatType.AVSLÅTT, BehandlingResultatType.INNVILGET);
    private static final Set<BehandlingResultatType> VEDTAKSTILSTANDER_KLAGE = Set.of(BehandlingResultatType.KLAGE_AVVIST,
            BehandlingResultatType.KLAGE_MEDHOLD, BehandlingResultatType.KLAGE_DELVIS_MEDHOLD, BehandlingResultatType.KLAGE_OMGJORT_UGUNST,
            BehandlingResultatType.KLAGE_YTELSESVEDTAK_OPPHEVET, BehandlingResultatType.KLAGE_YTELSESVEDTAK_STADFESTET,
            BehandlingResultatType.KLAGE_TILBAKEKREVING_VEDTAK_STADFESTET,
            BehandlingResultatType.HJEMSENDE_UTEN_OPPHEVE);
    private static final Set<BehandlingResultatType> VEDTAKSTILSTANDER_ANKE = Set.of(BehandlingResultatType.ANKE_AVVIST,
            BehandlingResultatType.ANKE_MEDHOLD, BehandlingResultatType.ANKE_DELVIS_MEDHOLD, BehandlingResultatType.ANKE_OMGJORT_UGUNST,
            BehandlingResultatType.ANKE_YTELSESVEDTAK_STADFESTET, BehandlingResultatType.ANKE_OPPHEVE_OG_HJEMSENDE,
            BehandlingResultatType.ANKE_HJEMSENDE_UTEN_OPPHEV);
    private static final Set<BehandlingResultatType> VEDTAKSTILSTANDER_INNSYN = Set.of(BehandlingResultatType.INNSYN_AVVIST,
            BehandlingResultatType.INNSYN_DELVIS_INNVILGET, BehandlingResultatType.INNSYN_INNVILGET);

    private static final Map<BehandlingType, Set<BehandlingResultatType>> LOVLIGE_RESULTAT = Map.ofEntries(
            Map.entry(BehandlingType.ANKE, VEDTAKSTILSTANDER_ANKE),
            Map.entry(BehandlingType.KLAGE, VEDTAKSTILSTANDER_KLAGE),
            Map.entry(BehandlingType.INNSYN, VEDTAKSTILSTANDER_INNSYN),
            Map.entry(BehandlingType.FØRSTEGANGSSØKNAD, VEDTAKSTILSTANDER),
            Map.entry(BehandlingType.REVURDERING, VEDTAKSTILSTANDER_REVURDERING));

    private LagretVedtakRepository lagretVedtakRepository;
    private FatteVedtakXmlTjeneste vedtakXmlTjeneste;
    private VedtakTjeneste vedtakTjeneste;
    private TotrinnTjeneste totrinnTjeneste;
    private BehandlingVedtakTjeneste behandlingVedtakTjeneste;
    private KlageAnkeVedtakTjeneste klageAnkeVedtakTjeneste;
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;

    FatteVedtakTjeneste() {
        // for CDI proxy
    }

    @Inject
    public FatteVedtakTjeneste(LagretVedtakRepository vedtakRepository,
                               KlageAnkeVedtakTjeneste klageAnkeVedtakTjeneste,
                               FatteVedtakXmlTjeneste vedtakXmlTjeneste,
                               VedtakTjeneste vedtakTjeneste, TotrinnTjeneste totrinnTjeneste,
                               BehandlingVedtakTjeneste behandlingVedtakTjeneste, BehandlingskontrollTjeneste behandlingskontrollTjeneste) {
        this.lagretVedtakRepository = vedtakRepository;
        this.klageAnkeVedtakTjeneste = klageAnkeVedtakTjeneste;
        this.vedtakXmlTjeneste = vedtakXmlTjeneste;
        this.vedtakTjeneste = vedtakTjeneste;
        this.totrinnTjeneste = totrinnTjeneste;
        this.behandlingVedtakTjeneste = behandlingVedtakTjeneste;
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
    }

    public BehandleStegResultat fattVedtak(BehandlingskontrollKontekst kontekst, Behandling behandling) {
        verifiserBehandlingsresultat(behandling);

        if (KlageAnkeVedtakTjeneste.behandlingErKlageEllerAnke(behandling) && klageAnkeVedtakTjeneste.erBehandletAvKabal(behandling)) {
            vedtakTjeneste.lagHistorikkinnslagFattVedtak(behandling);
            behandlingVedtakTjeneste.opprettBehandlingVedtak(kontekst, behandling);
            opprettLagretVedtak(behandling);
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        }

        if (behandling.isToTrinnsBehandling()) {
            var totrinnaksjonspunktvurderinger = totrinnTjeneste.hentTotrinnaksjonspunktvurderinger(behandling.getId());
            if (sendesTilbakeTilSaksbehandler(totrinnaksjonspunktvurderinger)) {
                var aksjonspunktDefinisjoner = totrinnaksjonspunktvurderinger.stream()
                        .filter(a -> !TRUE.equals(a.isGodkjent()))
                        .map(Totrinnsvurdering::getAksjonspunktDefinisjon).toList();
                if (ENV.isProd() || aksjonspunktDefinisjoner.stream().noneMatch(LEGACY_MEDLEM::contains)) {
                    return BehandleStegResultat.tilbakeførtMedAksjonspunkter(aksjonspunktDefinisjoner);
                } else if (aksjonspunktDefinisjoner.stream()
                    .anyMatch(ad -> behandlingskontrollTjeneste.sammenlignRekkefølge(behandling.getFagsakYtelseType(), behandling.getType(), ad.getBehandlingSteg(), BehandlingStegType.VURDER_MEDLEMSKAPVILKÅR) < 0)) {
                    return BehandleStegResultat.tilbakeførtMedAksjonspunkter(aksjonspunktDefinisjoner);
                } else {
                    return BehandleStegResultat.tilbakeførtMedlemskap();
                }


            }
        } else {
            vedtakTjeneste.lagHistorikkinnslagFattVedtak(behandling);
        }

        behandlingVedtakTjeneste.opprettBehandlingVedtak(kontekst, behandling);

        opprettLagretVedtak(behandling);

        // Ingen nye aksjonspunkt herfra
        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    private boolean sendesTilbakeTilSaksbehandler(Collection<Totrinnsvurdering> medTotrinnskontroll) {
        return medTotrinnskontroll.stream()
                .anyMatch(a -> !TRUE.equals(a.isGodkjent()));
    }

    private void verifiserBehandlingsresultat(Behandling behandling) {
        var behandlingsresultat = behandlingVedtakTjeneste.getBehandlingsresultat(behandling.getId());
        if (behandlingsresultat == null || !LOVLIGE_RESULTAT.get(behandling.getType()).contains(behandlingsresultat.getBehandlingResultatType())) {
            var exString = UTVIKLER_FEIL_VEDTAK + (behandlingsresultat == null ? "null" : behandlingsresultat.getBehandlingResultatType().getNavn());
            throw new IllegalStateException(exString);
        }
    }

    private void opprettLagretVedtak(Behandling behandling) {
        if (BehandlingType.INNSYN.equals(behandling.getType())) {
            return;
        }
        if (!erKlarForVedtak(behandling)) {
            var msg = String.format("Vedtak-XML kan ikke utarbeides for behandling %s i tilstand %s", behandling.getId(),
                behandling.getStatus().getKode());
            throw new TekniskException("FP-142918", msg);
        }
        var versjon = FPSAK_IMAGE != null && FPSAK_IMAGE.contains("fp-sak") ? FPSAK_IMAGE.substring(FPSAK_IMAGE.indexOf("fp-sak")) : null;
        var lagretVedtak = LagretVedtak.builder()
                .medBehandlingId(behandling.getId())
                .medFagsakId(behandling.getFagsakId())
                .medXmlClob(vedtakXmlTjeneste.opprettVedtakXml(behandling.getId()))
                .medFpsakVersjon(versjon)
                .build();
        lagretVedtakRepository.lagre(lagretVedtak);
    }

    private boolean erKlarForVedtak(Behandling behandling) {
        return BehandlingType.KLAGE.equals(behandling.getType()) || BehandlingType.ANKE.equals(behandling.getType())
                || BehandlingStatus.FATTER_VEDTAK.equals(behandling.getStatus());
    }

}
