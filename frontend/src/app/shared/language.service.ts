import {Injectable, signal} from '@angular/core';
import {TranslateService} from '@ngx-translate/core';

@Injectable({providedIn: 'root'})
export class LanguageService {
    private readonly STORAGE_KEY = 'lang';
    readonly current = signal<string>('en');

    constructor(private translate: TranslateService) {
        const saved = localStorage.getItem(this.STORAGE_KEY);
        const browserLang = translate.getBrowserLang() ?? 'en';
        const lang = saved ?? (['en', 'de'].includes(browserLang) ? browserLang : 'en');
        translate.addLangs(['en', 'de']);
        translate.setDefaultLang('en');
        translate.use(lang);
        this.current.set(lang);
    }

    setLanguage(lang: string): void {
        this.translate.use(lang);
        localStorage.setItem(this.STORAGE_KEY, lang);
        this.current.set(lang);
    }
}
