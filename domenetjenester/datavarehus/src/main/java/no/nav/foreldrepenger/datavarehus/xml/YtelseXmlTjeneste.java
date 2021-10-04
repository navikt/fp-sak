package no.nav.foreldrepenger.datavarehus.xml;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.vedtak.felles.xml.vedtak.v2.Beregningsresultat;

public interface YtelseXmlTjeneste {

    void setYtelse(Beregningsresultat beregningsresultat, Behandling behandling);
}
