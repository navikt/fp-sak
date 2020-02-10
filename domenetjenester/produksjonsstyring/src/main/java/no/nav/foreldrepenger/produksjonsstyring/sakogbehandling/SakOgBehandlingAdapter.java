package no.nav.foreldrepenger.produksjonsstyring.sakogbehandling;
/**
 * @deprecated single impl interface
 *
 */
@Deprecated
public interface SakOgBehandlingAdapter {

    void behandlingOpprettet(OpprettetBehandlingStatus status);
    void behandlingAvsluttet(AvsluttetBehandlingStatus status);
}
