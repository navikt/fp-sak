package no.nav.foreldrepenger.mottak.dokumentpersiterer.impl.søknad.v3;

import static no.nav.foreldrepenger.domene.typer.PersonIdent.erGyldigFnr;
import static no.nav.foreldrepenger.mottak.dokumentpersiterer.impl.søknad.v3.SøknadOversetter.finnLandkode;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.OppgittAnnenPartBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.OppgittAnnenPartEntitet;
import no.nav.foreldrepenger.domene.person.PersoninfoAdapter;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.vedtak.felles.xml.soeknad.engangsstoenad.v3.Engangsstønad;
import no.nav.vedtak.felles.xml.soeknad.felles.v3.AnnenForelder;
import no.nav.vedtak.felles.xml.soeknad.felles.v3.AnnenForelderMedNorskIdent;
import no.nav.vedtak.felles.xml.soeknad.felles.v3.AnnenForelderUtenNorskIdent;
import no.nav.vedtak.felles.xml.soeknad.felles.v3.UkjentForelder;
import no.nav.vedtak.felles.xml.soeknad.foreldrepenger.v3.Foreldrepenger;
import no.nav.vedtak.felles.xml.soeknad.kodeverk.v3.Land;

@ApplicationScoped
public class AnnenPartOversetter {

    private PersoninfoAdapter personinfoAdapter;

    @Inject
    public AnnenPartOversetter(PersoninfoAdapter personinfoAdapter) {
        this.personinfoAdapter = personinfoAdapter;
    }

    AnnenPartOversetter() {
        //CDI
    }

    Optional<OppgittAnnenPartEntitet> oversett(SøknadWrapper skjema, AktørId aktørIdSøker) {
        return extractAnnenForelder(skjema).map(annenForelder -> {
            if (annenForelder instanceof AnnenForelderMedNorskIdent annenForelderMedNorskIdent) {
                return map(annenForelderMedNorskIdent);

            }
            if (annenForelder instanceof AnnenForelderUtenNorskIdent annenForelderUtenNorskIdent) {
                return map(annenForelderUtenNorskIdent, aktørIdSøker);
            }
            throw new IllegalStateException("Ukjent type AnnenForelder " + annenForelder.getClass());
        });
    }

    private OppgittAnnenPartEntitet map(AnnenForelderUtenNorskIdent annenForelderUtenNorskIdent, AktørId aktørIdSøker) {
        var annenPartIdentString = annenForelderUtenNorskIdent.getUtenlandskPersonidentifikator().trim();
        var oppgittAnnenPartBuilder = new OppgittAnnenPartBuilder().medUtenlandskFnr(annenPartIdentString);
        if (erGyldigFnr(annenPartIdentString)) {
            personinfoAdapter.hentAktørForFnr(new PersonIdent(annenPartIdentString))
                .filter(a -> !aktørIdSøker.equals(a))
                .ifPresent(oppgittAnnenPartBuilder::medAktørId);
        }
        Optional.ofNullable(annenForelderUtenNorskIdent.getLand())
            .map(Land::getKode)
            .ifPresent(s -> oppgittAnnenPartBuilder.medUtenlandskFnrLand(finnLandkode(s)));
        return oppgittAnnenPartBuilder.build();
    }

    private static OppgittAnnenPartEntitet map(AnnenForelderMedNorskIdent annenForelderMedNorskIdent) {
        return new OppgittAnnenPartBuilder().medAktørId(new AktørId(annenForelderMedNorskIdent.getAktoerId())).build();
    }

    private static Optional<AnnenForelder> extractAnnenForelder(SøknadWrapper søknad) {
        AnnenForelder annenForelder = null;
        if (søknad.getOmYtelse() instanceof Foreldrepenger) {
            annenForelder = ((Foreldrepenger) søknad.getOmYtelse()).getAnnenForelder();
        } else if (søknad.getOmYtelse() instanceof Engangsstønad) {
            annenForelder = ((Engangsstønad) søknad.getOmYtelse()).getAnnenForelder();
        }
        //Håndterer UkjentForelder på lik måte som om forelder ikke var oppgitt
        if (annenForelder instanceof UkjentForelder) {
            return Optional.empty();
        }
        return Optional.ofNullable(annenForelder);
    }
}
