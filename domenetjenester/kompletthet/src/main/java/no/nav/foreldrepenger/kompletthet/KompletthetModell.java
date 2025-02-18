package no.nav.foreldrepenger.kompletthet;

import static java.util.stream.Collectors.toList;
import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.AUTO_KØET_BEHANDLING;
import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.AUTO_VENTER_PÅ_KOMPLETT_SØKNAD;
import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.AUTO_VENT_ETTERLYST_INNTEKTSMELDING;
import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.VENT_PGA_FOR_TIDLIG_SØKNAD;
import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.VENT_PÅ_SØKNAD;

import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;

@ApplicationScoped
public class KompletthetModell {

    private static final Map<AksjonspunktDefinisjon, BiFunction<KompletthetModell, ReferanseSkjæring, KompletthetResultat>> KOMPLETTHETSFUNKSJONER;

    static {
        Map<AksjonspunktDefinisjon, BiFunction<KompletthetModell, ReferanseSkjæring, KompletthetResultat>> map = new EnumMap<>(AksjonspunktDefinisjon.class);
        map.put(AUTO_VENTER_PÅ_KOMPLETT_SØKNAD, (kontroller, ref) -> finnKompletthetssjekker(kontroller).vurderForsendelseKomplett(ref.ref(), ref.stp()));
        map.put(VENT_PGA_FOR_TIDLIG_SØKNAD, (kontroller, ref) -> finnKompletthetssjekker(kontroller).vurderSøknadMottattForTidlig(ref.ref(), ref.stp()));
        map.put(VENT_PÅ_SØKNAD, (kontroller, ref) -> finnKompletthetssjekker(kontroller).vurderSøknadMottatt(ref.ref()));
        map.put(AUTO_VENT_ETTERLYST_INNTEKTSMELDING, (kontroller, ref) -> finnKompletthetssjekker(kontroller).vurderEtterlysningInntektsmelding(ref.ref(), ref.stp()));

        // Køet behandling kan inntreffe FØR kompletthetssteget er passert - men er ikke tilknyttet til noen kompletthetssjekk
        map.put(AUTO_KØET_BEHANDLING, (kontroller, behandling) -> KompletthetResultat.oppfylt());

        KOMPLETTHETSFUNKSJONER = Collections.unmodifiableMap(map);
    }

    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;
    private Kompletthetsjekker kompletthetsjekker;

    public KompletthetModell() {
        // For CDI proxy
    }

    @Inject
    public KompletthetModell(BehandlingskontrollTjeneste behandlingskontrollTjeneste, Kompletthetsjekker kompletthetsjekker) {
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
        this.kompletthetsjekker = kompletthetsjekker;
    }

    private static Kompletthetsjekker finnKompletthetssjekker(KompletthetModell kompletthetModell) {
        return kompletthetModell.kompletthetsjekker;
    }

    /**
     * Ranger autopunktene i kompletthetssjekk i samme rekkefølge som de ville ha blitt gjort i behandlingsstegene.
     * Dvs. at bruken av disse kompletthetssjekkene skjer UTENFOR behandlingsstegene, som introduserer risikoen for at
     * rekkefølgen avviker fra rekkefølgen INNE I behandlingsstegene. Bør være så enkel som mulig.
     **/
    // Rangering 1: Tidligste steg (dvs. autopunkt ville blitt eksekvert tidligst i behandlingsstegene)
    // TODO: Fjern denne hvis den ikke er nødvendig lenger.
    public List<AksjonspunktDefinisjon> rangerKompletthetsfunksjonerKnyttetTilAutopunkt(FagsakYtelseType ytelseType, BehandlingType behandlingType) {
        Comparator<AksjonspunktDefinisjon> stegRekkefølge = (apDef1, apDef2) ->
            behandlingskontrollTjeneste.sammenlignRekkefølge(ytelseType, behandlingType, apDef1.getBehandlingSteg(), apDef2.getBehandlingSteg());
        // Rangering 2: Autopunkt som kjøres igjen ved gjenopptakelse blir eksekvert FØR ikke-gjenopptagende i samme behandlingssteg
        //      Det er bare en implisitt antakelse at kodes riktig i stegene der Autopunkt brukes; bør forbedre dette.
        Comparator<AksjonspunktDefinisjon> tilbakehoppRekkefølge = (apDef1, apDef2) ->
            Boolean.compare(apDef1.tilbakehoppVedGjenopptakelse(), apDef2.tilbakehoppVedGjenopptakelse());

        return KOMPLETTHETSFUNKSJONER.keySet().stream()
            .filter(apdef -> apdef.getYtelseTyper().contains(ytelseType))
            .sorted(stegRekkefølge.thenComparing(tilbakehoppRekkefølge.reversed()))
            .collect(toList());
    }

    public boolean erTidligKompletthetssjekkPassert(Long behandlingId) {
        return behandlingskontrollTjeneste.erStegPassert(behandlingId, BehandlingStegType.VURDER_KOMPLETT_TIDLIG);
    }

    public boolean erRegisterinnhentingPassert(Long behandlingId) {
        return behandlingskontrollTjeneste.erStegPassert(behandlingId, BehandlingStegType.INNHENT_REGISTEROPP);
    }

    public boolean erKompletthetssjekkEllerPassert(Long behandlingId) {
        return behandlingskontrollTjeneste.erIStegEllerSenereSteg(behandlingId, BehandlingStegType.VURDER_KOMPLETT_TIDLIG);
    }

    public KompletthetResultat vurderKompletthet(BehandlingReferanse ref, Skjæringstidspunkt stp, List<AksjonspunktDefinisjon> åpneAksjonspunkter) {
        var åpentAutopunkt = åpneAksjonspunkter.stream()
            .findFirst();
        if (åpentAutopunkt.isPresent() && erAutopunktTilknyttetKompletthetssjekk(åpentAutopunkt)) {
            return vurderKompletthet(ref, stp, åpentAutopunkt.get());
        }
        if (!erTidligKompletthetssjekkPassert(ref.behandlingId())) {
            // Kompletthetssjekk er ikke passert, men står heller ikke på autopunkt tilknyttet kompletthet som skal sjekkes
            return KompletthetResultat.oppfylt();
        }
        // Default dersom ingen match på åpent autopunkt tilknyttet kompletthet OG kompletthetssjekk er passert
        var defaultAutopunkt = finnSisteAutopunktKnyttetTilKompletthetssjekk(ref);
        return vurderKompletthet(ref, stp, defaultAutopunkt);
    }

    private boolean erAutopunktTilknyttetKompletthetssjekk(Optional<AksjonspunktDefinisjon> åpentAutopunkt) {
        return åpentAutopunkt.filter(KOMPLETTHETSFUNKSJONER::containsKey).isPresent();
    }

    private AksjonspunktDefinisjon finnSisteAutopunktKnyttetTilKompletthetssjekk(BehandlingReferanse ref) {
        var rangerteAutopunkter = rangerKompletthetsfunksjonerKnyttetTilAutopunkt(ref.fagsakYtelseType(), ref.behandlingType());
        if (rangerteAutopunkter.isEmpty()) {
            throw new IllegalArgumentException("Utvklerfeil: Skal alltid finnes kompletthetsfunksjoner");
        }
        // Hent siste
        return rangerteAutopunkter.getLast();
    }

    public KompletthetResultat vurderKompletthet(BehandlingReferanse ref, Skjæringstidspunkt stp, AksjonspunktDefinisjon autopunkt) {
        return Optional.ofNullable(KOMPLETTHETSFUNKSJONER.get(autopunkt))
            .map(kompletthetsfunksjon -> kompletthetsfunksjon.apply(this, new ReferanseSkjæring(ref, stp)))
            .orElseThrow(() -> new IllegalStateException("Utviklerfeil: Kan ikke finne kompletthetsfunksjon for autopunkt: " + autopunkt.getKode()));
    }

    private record ReferanseSkjæring(BehandlingReferanse ref, Skjæringstidspunkt stp) {}
}
