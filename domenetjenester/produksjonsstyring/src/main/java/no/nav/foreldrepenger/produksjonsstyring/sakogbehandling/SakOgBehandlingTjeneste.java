package no.nav.foreldrepenger.produksjonsstyring.sakogbehandling;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

@Dependent
public class SakOgBehandlingTjeneste {

    private SakOgBehandlingAdapter adapter;

    public SakOgBehandlingTjeneste(){
        //for CDI
    }

    @Inject
    public SakOgBehandlingTjeneste(SakOgBehandlingAdapter adapter) {
        this.adapter = adapter;
    }

    public void behandlingOpprettet(OpprettetBehandlingStatus status) {
        adapter.behandlingOpprettet(status);
    }

    public void behandlingAvsluttet(AvsluttetBehandlingStatus status) {
        adapter.behandlingAvsluttet(status);
    }

}
