package no.nav.foreldrepenger.domene.medlem.impl;

import jakarta.enterprise.context.ApplicationScoped;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.RegisterdataDiffsjekker;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;

@FagsakYtelseTypeRef(FagsakYtelseType.FORELDREPENGER)
@ApplicationScoped
public class MedlemEndringssjekkerForeldrepenger extends MedlemEndringssjekker {

    @Override
    public RegisterdataDiffsjekker opprettNyDiffer() {
        return new RegisterdataDiffsjekker();
    }
}
