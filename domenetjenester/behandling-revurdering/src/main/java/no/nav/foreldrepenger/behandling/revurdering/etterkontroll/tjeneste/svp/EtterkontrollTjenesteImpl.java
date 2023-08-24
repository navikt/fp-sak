package no.nav.foreldrepenger.behandling.revurdering.etterkontroll.tjeneste.svp;

import jakarta.enterprise.context.ApplicationScoped;
import no.nav.foreldrepenger.behandling.revurdering.etterkontroll.tjeneste.EtterkontrollTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.aktør.FødtBarnInfo;
import no.nav.foreldrepenger.behandlingslager.aktør.OrganisasjonsEnhet;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
@FagsakYtelseTypeRef(FagsakYtelseType.SVANGERSKAPSPENGER)
public class EtterkontrollTjenesteImpl implements EtterkontrollTjeneste {

    @Override
    public Optional<BehandlingÅrsakType> utledRevurderingÅrsak(Behandling behandling, FamilieHendelseGrunnlagEntitet grunnlag,
            List<FødtBarnInfo> barnFraRegister) {
        throw new IllegalStateException("Utviklerfeil: Det er ikke meningen at denne skal bli kalt");
    }

    @Override
    public void opprettRevurdering(Behandling behandling, BehandlingÅrsakType årsak, OrganisasjonsEnhet enhetForRevurdering) {
        throw new IllegalStateException("Utviklerfeil: Det er ikke meningen at denne skal bli kalt");
    }
}
