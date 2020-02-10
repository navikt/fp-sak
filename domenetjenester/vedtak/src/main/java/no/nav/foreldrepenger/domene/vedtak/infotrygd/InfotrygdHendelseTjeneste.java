package no.nav.foreldrepenger.domene.vedtak.infotrygd;

import java.util.List;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;

public interface InfotrygdHendelseTjeneste {

    List<InfotrygdHendelse> hentHendelsesListFraInfotrygdFeed(Behandling behandling);

}
