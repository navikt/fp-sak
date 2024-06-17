package no.nav.foreldrepenger.behandling.revurdering.etterkontroll.tjeneste;

import java.util.List;
import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.aktør.FødtBarnInfo;
import no.nav.foreldrepenger.behandlingslager.aktør.OrganisasjonsEnhet;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;

public interface EtterkontrollTjeneste {

    Optional<BehandlingÅrsakType> utledRevurderingÅrsak(Behandling behandling,
                                                        FamilieHendelseGrunnlagEntitet grunnlag,
                                                        List<FødtBarnInfo> barnFraRegister);

    void opprettRevurdering(Behandling behandling, BehandlingÅrsakType årsak, OrganisasjonsEnhet enhetForRevurdering);

}
