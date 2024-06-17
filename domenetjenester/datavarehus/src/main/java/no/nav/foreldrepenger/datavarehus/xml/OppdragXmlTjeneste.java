package no.nav.foreldrepenger.datavarehus.xml;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.vedtak.felles.xml.vedtak.v2.Vedtak;


public interface OppdragXmlTjeneste {
    void setOppdrag(Vedtak vedtak, Behandling behandling);
}
