package no.nav.foreldrepenger.mottak.hendelser.oversetter;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.hendelser.ForretningshendelseType;
import no.nav.foreldrepenger.mottak.hendelser.ForretningshendelseOversetter;
import no.nav.foreldrepenger.mottak.hendelser.ForretningshendelsestypeRef;
import no.nav.foreldrepenger.mottak.hendelser.JsonMapper;
import no.nav.foreldrepenger.mottak.hendelser.kontrakt.ForretningshendelseDto;
import no.nav.foreldrepenger.mottak.hendelser.kontrakt.YtelseHendelse;
import no.nav.foreldrepenger.mottak.ytelse.YtelseForretningshendelse;

@ApplicationScoped
@ForretningshendelsestypeRef(ForretningshendelsestypeRef.YTELSE_HENDELSE)
public class YtelseForretningshendelseOversetter implements ForretningshendelseOversetter<YtelseForretningshendelse> {

    @Inject
    public YtelseForretningshendelseOversetter() {
    }

    @Override
    public YtelseForretningshendelse oversett(ForretningshendelseDto forretningshendelse) {
        ForretningshendelseType forretningshendelseType = ForretningshendelseType.fraKode(forretningshendelse.getForretningshendelseType());

        YtelseHendelse ytelseHendelse = JsonMapper.fromJson(forretningshendelse.getPayloadJson(), YtelseHendelse.class);
        return new YtelseForretningshendelse(forretningshendelseType, ytelseHendelse.getAktoerId(), ytelseHendelse.getFom());
    }
}
