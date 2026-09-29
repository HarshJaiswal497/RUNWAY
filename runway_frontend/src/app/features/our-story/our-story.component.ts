import { Component, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-our-story',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './our-story.component.html',
  styleUrl: './our-story.component.css'
})
export class OurStoryComponent implements OnInit {

  readonly designers = [
    {
      num: '01',
      name: 'Aurèle Maison',
      focus: 'Fine Accessories',
      desc: 'Jewellery and accessories conceived at the intersection of sculpture and adornment. Each piece a meditation on form.'
    },
    {
      num: '02',
      name: 'Vérin Atelier',
      focus: 'Leather Goods',
      desc: 'Vegetable-tanned leathers and brushed metals. Bags and accessories that age with intention.'
    },
    {
      num: '03',
      name: 'Solen Maison',
      focus: "Women's Footwear",
      desc: 'Architectural heels and refined flats. Footwear that commands a room before a word is spoken.'
    },
    {
      num: '04',
      name: 'Kade Atelier',
      focus: "Men's Footwear",
      desc: 'Oxford constructions and Chelsea silhouettes. Precision-lasted shoes for those who understand the foundation of a wardrobe.'
    },
    {
      num: '05',
      name: 'Maison Noir',
      focus: "Men's & Unisex Apparel",
      desc: 'Tailored suiting and structured outerwear. The essential vocabulary of the modern wardrobe, spoken fluently.'
    },
    {
      num: '06',
      name: 'Atelier Regale',
      focus: "Women's Apparel",
      desc: 'Evening gowns and ceremonial dressing. Garments designed not for occasions, but for the moments that define them.'
    },
  ];

  ngOnInit(): void {
    window.scrollTo({ top: 0, behavior: 'instant' });
  }
}