package no.nav.foreldrepenger.mottak.kompletthettjeneste;

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
import no.nav.foreldrepenger.kompletthet.KompletthetResultat;
import no.nav.foreldrepenger.kompletthet.Kompletthetsjekker;
import no.nav.foreldrepenger.kompletthet.KompletthetsjekkerProvider;

@ApplicationScoped
public class KompletthetModell {

    private static final Map<AksjonspunktDefinisjon, BiFunction<KompletthetModell, ReferanseSkjæring, KompletthetResultat>> KOMPLETTHETSFUNKSJONER;

    static {
        Map<AksjonspunktDefinisjon, BiFunction<KompletthetModell, ReferanseSkjæring, KompletthetResultat>> map = new EnumMap<>(AksjonspunktDefinisjon.class);
        map.put(AUTO_VENTER_PÅ_KOMPLETT_SØKNAD, (kontroller, ref) -> finnKompletthetssjekker(kontroller, ref).vurderForsendelseKomplett(ref.ref(), ref.stp()));
        map.put(VENT_PGA_FOR_TIDLIG_SØKNAD, (kontroller, ref) -> finnKompletthetssjekker(kontroller, ref).vurderSøknadMottattForTidlig(ref.stp()));
        map.put(VENT_PÅ_SØKNAD, (kontroller, ref) -> finnKompletthetssjekker(kontroller, ref).vurderSøknadMottatt(ref.ref()));
        map.put(AUTO_VENT_ETTERLYST_INNTEKTSMELDING, (kontroller, ref) -> finnKompletthetssjekker(kontroller, ref).vurderEtterlysningInntektsmelding(ref.ref(), ref.stp()));

        // Køet behandling kan inntreffe FØR kompletthetssteget er passert - men er ikke tilknyttet til noen kompletthetssjekk
        map.put(AUTO_KØET_BEHANDLING, (kontroller, behandling) -> KompletthetResultat.oppfylt());

        KOMPLETTHETSFUNKSJONER = Collections.unmodifiableMap(map);
    }

    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;
    private KompletthetsjekkerProvider kompletthetsjekkerProvider;

    public KompletthetModell() {
        // For CDI proxy
    }

    @Inject
    public KompletthetModell(BehandlingskontrollTjeneste behandlingskontrollTjeneste,
                             KompletthetsjekkerProvider kompletthetsjekkerProvider) {
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
        this.kompletthetsjekkerProvider = kompletthetsjekkerProvider;
    }

    private static Kompletthetsjekker finnKompletthetssjekker(KompletthetModell kompletthetModell, ReferanseSkjæring rs) {
        return kompletthetModell.kompletthetsjekkerProvider.finnKompletthetsjekkerFor(rs.ref().fagsakYtelseType(), rs.ref().behandlingType());
    }

    /**
     * Ranger autopunktene i kompletthetssjekk i samme rekkefølge som de ville ha blitt gjort i behandlingsstegene.
     * Dvs. at bruken av disse kompletthetssjekkene skjer UTENFOR behandlingsstegene, som introduserer risikoen for at
     * rekkefølgen avviker fra rekkefølgen INNE I behandlingsstegene. Bør være så enkel som mulig.
     **/
    // Rangering 1: Tidligste steg (dvs. autopunkt ville blitt eksekvert tidligst i behandlingsstegene)
    public List<AksjonspunktDefinisjon> rangerKompletthetsfunksjonerKnyttetTilAutopunkt(FagsakYtelseType ytelseType, BehandlingType behandlingType) {
        Comparator<AksjonspunktDefinisjon> stegRekkefølge = (apDef1, apDef2) ->
            behandlingskontrollTjeneste.sammenlignRekkefølge(ytelseType, behandlingType, apDef1.getBehandlingSteg(), apDef2.getBehandlingSteg());
        // Rangering 2: Autopunkt som kjøres igjen ved gjenopptakelse blir eksekvert FØR ikke-gjenopptagende i samme behandlingssteg
        //      Det er bare en implisitt antakelse at kodes riktig i stegene der Autopunkt brukes; bør forbedre dette.
        Comparator<AksjonspunktDefinisjon> tilbakehoppRekkefølge = (apDef1, apDef2) ->
            Boolean.compare(apDef1.tilbakehoppVedGjenopptakelse(), apDef2.tilbakehoppVedGjenopptakelse());

        return KOMPLETTHETSFUNKSJONER.keySet().stream()
            .filter(apdef -> apdef.getYtelseTyper().contains(ytelseType))
            .sorted(stegRekkefølge
                .thenComparing(tilbakehoppRekkefølge.reversed()))
            .collect(toList());
    }

    public boolean erKompletthetssjekkPassert(Long behandlingId) {
        return behandlingskontrollTjeneste.erStegPassert(behandlingId, BehandlingStegType.VURDER_KOMPLETTHET);
    }

    public boolean erKompletthetssjekkEllerPassert(Long behandlingId) {
        return behandlingskontrollTjeneste.erIStegEllerSenereSteg(behandlingId, BehandlingStegType.VURDER_KOMPLETTHET);
    }

    public KompletthetResultat vurderKompletthet(BehandlingReferanse ref, Skjæringstidspunkt stp, List<AksjonspunktDefinisjon> åpneAksjonspunkter) {
        var åpentAutopunkt = åpneAksjonspunkter.stream()
            .findFirst();
        if (åpentAutopunkt.isPresent() && erAutopunktTilknyttetKompletthetssjekk(åpentAutopunkt)) {
            return vurderKompletthet(ref, stp, åpentAutopunkt.get());
        }
        if (!erKompletthetssjekkPassert(ref.behandlingId())) {
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
