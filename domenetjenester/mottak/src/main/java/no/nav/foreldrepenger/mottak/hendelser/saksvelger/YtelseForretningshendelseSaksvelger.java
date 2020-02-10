package no.nav.foreldrepenger.mottak.hendelser.saksvelger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.hendelser.ForretningshendelseType;
import no.nav.foreldrepenger.mottak.hendelser.ForretningshendelseSaksvelger;
import no.nav.foreldrepenger.mottak.hendelser.ForretningshendelsestypeRef;
import no.nav.foreldrepenger.mottak.ytelse.YtelseForretningshendelse;


@ForretningshendelsestypeRef(ForretningshendelsestypeRef.YTELSE_HENDELSE)
@ApplicationScoped
public class YtelseForretningshendelseSaksvelger implements ForretningshendelseSaksvelger<YtelseForretningshendelse> {

    private static Map<ForretningshendelseType, BehandlingÅrsakType> HENDELSE_TIL_BEHANDLINGSÅRSAK = new HashMap<>();
    static {
        HENDELSE_TIL_BEHANDLINGSÅRSAK.put(ForretningshendelseType.YTELSE_INNVILGET, BehandlingÅrsakType.RE_TILSTØTENDE_YTELSE_INNVILGET);
        HENDELSE_TIL_BEHANDLINGSÅRSAK.put(ForretningshendelseType.YTELSE_ENDRET, BehandlingÅrsakType.RE_ENDRING_BEREGNINGSGRUNNLAG);
        HENDELSE_TIL_BEHANDLINGSÅRSAK.put(ForretningshendelseType.YTELSE_OPPHØRT, BehandlingÅrsakType.RE_TILSTØTENDE_YTELSE_OPPHØRT);
        HENDELSE_TIL_BEHANDLINGSÅRSAK.put(ForretningshendelseType.YTELSE_ANNULERT, BehandlingÅrsakType.RE_TILSTØTENDE_YTELSE_OPPHØRT);
    }

    private FagsakRepository fagsakRepository;

    public YtelseForretningshendelseSaksvelger() {
        //Criminal Diamonds Inc.
    }

    @Inject
    public YtelseForretningshendelseSaksvelger(BehandlingRepositoryProvider provider) {
        this.fagsakRepository = provider.getFagsakRepository();
    }

    @Override
    public Map<BehandlingÅrsakType, List<Fagsak>> finnRelaterteFagsaker(YtelseForretningshendelse forretningshendelse) {
        Map<BehandlingÅrsakType, List<Fagsak>> resultat = new HashMap<>();
        BehandlingÅrsakType behandlingÅrsakType = finnBehandlingÅrsakType(forretningshendelse.getForretningshendelseType());
        resultat.put(behandlingÅrsakType, fagsakRepository.hentForBruker(forretningshendelse.getAktørId()).stream()
            .filter(Fagsak::erÅpen)
            .filter(fagsak -> fagsak.getYtelseType().equals(FagsakYtelseType.FORELDREPENGER))
            .collect(Collectors.toList()));
        return resultat;
    }

    private BehandlingÅrsakType finnBehandlingÅrsakType(ForretningshendelseType forretningshendelseType) {
        return HENDELSE_TIL_BEHANDLINGSÅRSAK.getOrDefault(forretningshendelseType, BehandlingÅrsakType.UDEFINERT);
    }
}

