package no.nav.foreldrepenger.behandling.revurdering;

import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.aktør.OrganisasjonsEnhet;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;

public interface RevurderingTjeneste {

    Behandling opprettManuellRevurdering(Fagsak fagsak, BehandlingÅrsakType revurderingsÅrsak, Optional<OrganisasjonsEnhet> enhet);

    Behandling opprettAutomatiskRevurdering(Fagsak fagsak, BehandlingÅrsakType revurderingsÅrsak, Optional<OrganisasjonsEnhet> enhet);

    void kopierAlleGrunnlagFraTidligereBehandling(Behandling original, Behandling ny);

    Boolean kanRevurderingOpprettes(Fagsak fagsak);

    boolean erRevurderingMedUendretUtfall(Behandling behandling);

}
