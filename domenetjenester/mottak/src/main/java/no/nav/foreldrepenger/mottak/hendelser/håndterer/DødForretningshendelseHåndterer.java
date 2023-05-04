package no.nav.foreldrepenger.mottak.hendelser.håndterer;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRevurderingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.hendelser.ForretningshendelseType;
import no.nav.foreldrepenger.mottak.hendelser.ForretningshendelseHåndterer;
import no.nav.foreldrepenger.mottak.hendelser.ForretningshendelsestypeRef;
import no.nav.foreldrepenger.mottak.hendelser.ToForeldreBarnDødTjeneste;

@ApplicationScoped
@ForretningshendelsestypeRef(ForretningshendelseType.DØD)
public class DødForretningshendelseHåndterer implements ForretningshendelseHåndterer {

    private ForretningshendelseHåndtererFelles forretningshendelseHåndtererFelles;
    private BehandlingRevurderingRepository behandlingRevurderingRepository;
    private ToForeldreBarnDødTjeneste toForeldreBarnDødTjeneste;
    private BehandlingRepository behandlingRepository;

    @Inject
    public DødForretningshendelseHåndterer(BehandlingRepositoryProvider repositoryProvider,
                                           ForretningshendelseHåndtererFelles forretningshendelseHåndtererFelles,
                                           ToForeldreBarnDødTjeneste toForeldreBarnDødTjeneste) {
        this.forretningshendelseHåndtererFelles = forretningshendelseHåndtererFelles;
        this.behandlingRevurderingRepository = repositoryProvider.getBehandlingRevurderingRepository();
        this.toForeldreBarnDødTjeneste = toForeldreBarnDødTjeneste;
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();

    }

    @Override
    public void håndterÅpenBehandling(Behandling åpenBehandling, BehandlingÅrsakType behandlingÅrsakType) {
        if (BehandlingÅrsakType.RE_HENDELSE_DØD_BARN.equals(behandlingÅrsakType)
            && forretningshendelseHåndtererFelles.barnFødselogDødAlleredeRegistrert(åpenBehandling)) {
            return;
        }
        forretningshendelseHåndtererFelles.håndterÅpenBehandling(åpenBehandling, behandlingÅrsakType);
    }

    @Override
    public void håndterAvsluttetBehandling(Behandling avsluttetBehandling, ForretningshendelseType forretningshendelseType, BehandlingÅrsakType behandlingÅrsakType) {
        // Vi vet nå at denne behandlingen er avsluttet og innvilget, og at dersom det finnes en åpen behandling på medforelder, så er den køet
        // Hvis det er barnet som har dødd må begge foreldrenes saker revurderes.
        // Revurderer forelderen med nærmest uttak først. Velges v.h.a ToForeldreBarnDødTjeneste
        if (BehandlingÅrsakType.RE_HENDELSE_DØD_BARN.equals(behandlingÅrsakType)) {
            if (forretningshendelseHåndtererFelles.barnFødselogDødAlleredeRegistrert(avsluttetBehandling)) {
                return;
            }
            var fagsakPåMedforelder = behandlingRevurderingRepository.finnFagsakPåMedforelder(avsluttetBehandling.getFagsak());
            Optional<Behandling> behandlingPåMedforelder = fagsakPåMedforelder.isPresent()?
                behandlingRepository.finnSisteIkkeHenlagteYtelseBehandlingFor(fagsakPåMedforelder.get().getId())
                : Optional.empty();
            if (behandlingPåMedforelder.isPresent()
                && !behandlingPåMedforelder.get().erKøet()
                && skalOppretteRevurderingPåMedForelderFørst(avsluttetBehandling, behandlingPåMedforelder.get())) {
                // Dette er annen parts fagsak, og mor har en fagsak
                håndterKøetBehandling(avsluttetBehandling.getFagsak(), behandlingÅrsakType);
                return;
            }
        }
        // Dette er mors fagsak, eller dette er annen parts fagsak og mor har ingen fagsak, eller det er en av foreldrene som er døde
        forretningshendelseHåndtererFelles.opprettRevurderingLagStartTask(avsluttetBehandling.getFagsak(), behandlingÅrsakType);
    }

    @Override
    public void håndterKøetBehandling(Fagsak fagsak, BehandlingÅrsakType behandlingÅrsakType) {
        var køetBehandlingOpt = behandlingRevurderingRepository.finnKøetYtelsesbehandling(fagsak.getId());
        if (BehandlingÅrsakType.RE_HENDELSE_DØD_BARN.equals(behandlingÅrsakType)
            && køetBehandlingOpt.filter(forretningshendelseHåndtererFelles::barnFødselogDødAlleredeRegistrert).isPresent()) {
            return;
        }
        forretningshendelseHåndtererFelles.håndterKøetBehandling(fagsak, behandlingÅrsakType, køetBehandlingOpt);
    }

    private boolean skalOppretteRevurderingPåMedForelderFørst(Behandling behandling, Behandling behandlingPåMedforelder) {
        return behandlingPåMedforelder == toForeldreBarnDødTjeneste.finnBehandlingSomSkalRevurderes(behandling, behandlingPåMedforelder);
    }
}
